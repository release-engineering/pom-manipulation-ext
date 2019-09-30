/*
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.core.impl;

import org.apache.maven.artifact.ArtifactScopeEnum;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleTypeAndClassifier;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.model.SimpleScopedArtifactRef;
import org.commonjava.maven.ext.common.util.PropertyResolver;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.DependencyState;
import org.commonjava.maven.ext.core.state.PluginState;
import org.commonjava.maven.ext.core.state.RESTState;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.core.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * This Manipulator runs very early. It makes a REST call to an external service to loadRemoteOverrides the GAVs to align the project version
 * and dependencies to. It will prepopulate Project GA versions into the VersioningState in case the VersioningManipulator has been activated
 * and the remote overrides into the DependencyState for those as well.
 */
@Named("rest-manipulator")
@Singleton
public class RESTCollector
                implements Manipulator
{
    private static final Logger logger = LoggerFactory.getLogger( RESTCollector.class );

    private ManipulationSession session;

    @Override
    public void init( final ManipulationSession session ) throws ManipulationException
    {
        this.session = session;
        session.setState( new RESTState( session ) );
    }

    /**
     * Prescans the Project to build up a list of Project GAs and also the various Dependencies.
     */
    private void collect( final List<Project> projects )
                    throws ManipulationException
    {
        final RESTState state = session.getState( RESTState.class );
        final VersioningState vs = session.getState( VersioningState.class );
        final DependencyState ds = session.getState( DependencyState.class );
        final PluginState ps = session.getState( PluginState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug("{}: Nothing to do!", getClass().getSimpleName());
            return;
        }

        final ArrayList<ProjectVersionRef> newProjectKeys = new ArrayList<>();
        final String override = vs.getOverride();

        for ( final Project project : projects )
        {
            if ( isEmpty( override ) )
            {
                // TODO: Check this : For the rest API I think we need to check every project GA not just inheritance root.
                // Strip SNAPSHOT and handle alternate suffixes from the version for matching. DA will handle OSGi conversion.
                newProjectKeys.add( new SimpleProjectVersionRef(
                                project.getKey().asProjectRef(), handlePotentialSnapshotVersion(
                                                vs, VersionCalculator.handleAlternate( vs, project.getVersion() ) ) ) );
            }
            else if ( project.isExecutionRoot() )
            {
                // We want to manually override the version ; therefore ignore what is in the project and calculate potential
                // matches for that instead.
                Project p = projects.get( 0 );
                newProjectKeys.add( new SimpleProjectVersionRef( p.getGroupId(), p.getArtifactId(), override ) );
            }
        }

        final Set<ProjectVersionRef> restParamSet = new HashSet<>( newProjectKeys );
        final Set<ArtifactRef> localDeps = establishAllDependencies( session, projects, null );

        // Ok we now have a defined list of top level project plus a unique list of all possible dependencies.
        // Need to send that to the rest interface to get a translation.
        for ( ArtifactRef p : localDeps )
        {
            restParamSet.add( p.asProjectVersionRef() );
        }
        final List<ProjectVersionRef> restParam = new ArrayList<>( restParamSet );

        // Call the REST to populate the result.
        logger.debug ("Passing {} GAVs into the REST client api {} ", restParam.size(), restParam);
        Map<ProjectVersionRef, String> restResult = state.getVersionTranslator().translateVersions( restParam );
        logger.info ("REST Client returned {} ", restResult);

        vs.setRESTMetadata (parseVersions(session, projects, state, newProjectKeys, restResult));

        final Map<ArtifactRef, String> overrides = new HashMap<>();

        // Convert the loaded remote ProjectVersionRefs to the original ArtifactRefs
        for (ArtifactRef a : localDeps )
        {
            if (restResult != null && restResult.containsKey( a.asProjectVersionRef() ))
            {
                overrides.put( a, restResult.get( a.asProjectVersionRef()));
            }
        }
        logger.debug( "Setting REST Overrides {} ", overrides );
        ds.setRemoteRESTOverrides( overrides );
        // Unfortunately as everything is just GAVs we have to send everything to the PluginManipulator as well.
        ps.setRemoteRESTOverrides( overrides );
    }

    /**
     * Parse the rest result for the project GAs and store them in versioning state for use
     * there by incremental suffix calculation.
     */
    private Map<ProjectRef, Set<String>> parseVersions( ManipulationSession session, List<Project> projects, RESTState state,
                                                        List<ProjectVersionRef> newProjectKeys, Map<ProjectVersionRef, String> restResult )
                    throws ManipulationException
    {
        Map<ProjectRef, Set<String>> versionStates = new HashMap<>();
        for ( final ProjectVersionRef p : newProjectKeys )
        {
            if ( restResult.containsKey( p ) )
            {
                // Found part of the current project to store in Versioning State
                Set<String> versions = versionStates.computeIfAbsent( p.asProjectRef(), k -> new HashSet<>() );
                versions.add( restResult.get( p ) );
            }
        }
        logger.debug ("Added the following ProjectRef:Version from REST call into VersionState {}", versionStates);

        // We know we have ProjectVersionRef(s) of the current project(s). We need to establish potential
        // blacklist by calling
        // GET /listings/blacklist/ga?groupid=GROUP_ID&artifactid=ARTIFACT_ID
        // passing in the groupId and artifactId.

        // From the results we then need to establish whether the community version occurs in the blacklist
        // causing a total abort and whether any redhat versions occur in the blacklist. If they do, that will
        // affect the incremental potential options. The simplest option is simply to add those results to versionStates
        // list. This will cause the incremental build number to be set to greater than those.

        List<ProjectVersionRef> blacklist;

        for ( Project p : projects )
        {
            if ( p.isExecutionRoot() )
            {
                logger.debug ("Calling REST client for blacklist with {}...", p.getKey().asProjectRef());
                blacklist = state.getVersionTranslator().findBlacklisted( p.getKey().asProjectRef() );

                if ( blacklist.size() > 0)
                {
                    String suffix = PropertiesUtils.getSuffix( session );
                    String bVersion = blacklist.get( 0 ).getVersionString();
                    String pVersion = p.getVersion();
                    logger.debug( "REST Client returned for blacklist {} ", blacklist );

                    if ( isEmpty( suffix ) )
                    {
                        logger.warn( "No version suffix found ; unable to verify community blacklisting." );
                    }
                    else if ( blacklist.size() == 1 && !bVersion.contains( suffix ) )
                    {
                        if ( pVersion.contains( suffix ) )
                        {
                            pVersion = pVersion.substring( 0, pVersion.indexOf( suffix ) - 1 );
                        }
                        if ( pVersion.equals( bVersion ) )
                        {
                            throw new ManipulationException( "community artifact '" + blacklist.get( 0 ) + "' has been blacklisted. Unable to build project version "
                                                                             + p.getVersion() );
                        }
                    }

                    // Found part of the current project to store in Versioning State
                    Set<String> versions = versionStates.computeIfAbsent( p.getKey().asProjectRef(), k -> new HashSet<>() );
                    for ( ProjectVersionRef b : blacklist )
                    {
                        versions.add( b.getVersionString() );
                    }

                }
                // else no blacklisted artifacts so just continue

                break;
            }
        }


        return versionStates;
    }

    /**
     * No-op in this case - any changes, if configured, would happen in Versioning or Dependency Manipulators.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
                    throws ManipulationException
    {
        collect( projects );

        return Collections.emptySet();
    }

    @Override
    public int getExecutionIndex()
    {
        // Low value index so it runs very early in order to call the REST API.
        return 10;
    }


    /**
     * Scans a list of projects and accumulates all dependencies and returns them.
     *
     * @param session the ManipulationSession
     * @param projects the projects to scan.
     * @param activeProfiles which profiles to check
     * @return an unsorted set of ArtifactRefs used.
     * @throws ManipulationException if an error occurs
     */
    public static Set<ArtifactRef> establishAllDependencies( ManipulationSession session, final List<Project> projects, Set<String> activeProfiles ) throws ManipulationException
    {
        final VersioningState vs = session.getState( VersioningState.class );

        Set<ArtifactRef> localDeps = new HashSet<>();
        Set<String> activeModules = new HashSet<>();
        boolean scanAll = false;

        if ( activeProfiles != null && !activeProfiles.isEmpty() )
        {
            for ( final Project project : projects )
            {
                if ( project.isInheritanceRoot() )
                {
                    activeModules.addAll( project.getModel().getModules() );

                    List<Profile> profiles = project.getModel().getProfiles();

                    if ( profiles != null )
                    {
                        for ( Profile p : profiles )
                        {
                            if ( activeProfiles.contains( p.getId() ) )
                            {
                                logger.debug( "Adding modules for profile {}", p.getId() );
                                activeModules.addAll( p.getModules() );
                            }
                        }
                    }
                }
            }
            logger.debug( "Found {} active modules with {} active profiles.", activeModules, activeProfiles );
        }
        else
        {
            scanAll = true;
        }

        // Iterate over current project set and populate list of dependencies
        for ( final Project project : projects )
        {
            if ( project.isInheritanceRoot() || scanAll || activeModules.contains( project.getPom().getParentFile().getName() ) )
            {
                if ( project.getModelParent() != null )
                {
                    SimpleProjectVersionRef parent = new SimpleProjectVersionRef( project.getModelParent().getGroupId(),
                                                                                  project.getModelParent().getArtifactId(),
                                                                                  handlePotentialSnapshotVersion( vs, project.getModelParent().getVersion() ) );
                    localDeps.add( new SimpleArtifactRef( parent, new SimpleTypeAndClassifier( "pom", null ) ) );
                }

                recordDependencies( session, project, localDeps, project.getResolvedManagedDependencies( session ) );
                recordDependencies( session, project, localDeps, project.getResolvedDependencies( session ) );
                recordPlugins( session, localDeps, project.getResolvedManagedPlugins( session ) );
                recordPlugins( session, localDeps, project.getResolvedPlugins( session ) );

                List<Profile> profiles = project.getModel().getProfiles();
                if ( profiles != null )
                {
                    for ( Profile p : profiles )
                    {
                        if ( !scanAll && !activeProfiles.contains( p.getId() ) )
                        {
                            continue;
                        }
                        recordDependencies( session, project, localDeps,
                                            project.getResolvedProfileManagedDependencies( session ).getOrDefault (p, Collections.emptyMap()) );
                        recordDependencies( session, project, localDeps,
                                             project.getResolvedProfileDependencies( session ).getOrDefault( p, Collections.emptyMap() ) );
                        recordPlugins( session, localDeps, project.getResolvedProfileManagedPlugins( session ).getOrDefault( p, Collections.emptyMap() ) );
                        recordPlugins( session, localDeps, project.getResolvedProfilePlugins( session ).getOrDefault( p, Collections.emptyMap() ) );

                    }
                }
            }
        }
        return localDeps;
    }

    /**
     * Translate a given set of pvr:plugins into ArtifactRefs.
     * @param session the current Session.
     * @param deps the list of ArtifactRefs to return
     * @param plugins the plugins to transform.
     */
    private static void recordPlugins( ManipulationSession session, Set<ArtifactRef> deps, Map<ProjectVersionRef, Plugin> plugins )
    {
        final VersioningState vs = session.getState( VersioningState.class );

        for ( ProjectVersionRef pvr : plugins.keySet() )
        {
            deps.add( new SimpleScopedArtifactRef(
                            new SimpleProjectVersionRef( pvr.asProjectRef(), handlePotentialSnapshotVersion( vs, pvr.getVersionString() ) ),
                            new SimpleTypeAndClassifier( "maven-plugin", null ), ArtifactScopeEnum.compile.name() ) );
        }
    }


    /**
     * Translate a given set of pvr:dependencies into ArtifactRefs.
     * @param session the ManipulationSession
     * @param project currently scanned project
     * @param deps Set of ArtifactRef to store the results in.
     * @param dependencies dependencies to examine
     */
    private static void recordDependencies( ManipulationSession session, Project project, Set<ArtifactRef> deps,
                                            Map<ArtifactRef, Dependency> dependencies )
                    throws ManipulationException
    {
        final VersioningState vs = session.getState( VersioningState.class );
        final RESTState state = session.getState( RESTState.class );

        for ( ArtifactRef pvr : dependencies.keySet() )
        {
            Dependency d = dependencies.get( pvr );
            SimpleScopedArtifactRef sa = new SimpleScopedArtifactRef(
                            new SimpleProjectVersionRef( pvr.asProjectRef(), handlePotentialSnapshotVersion( vs, pvr.getVersionString() ) ),
                            new SimpleTypeAndClassifier( d.getType(), d.getClassifier() ), isEmpty( d.getScope() ) ?
                                                                   ArtifactScopeEnum.compile.name() :
                                                                   PropertyResolver.resolveInheritedProperties( session,
                                                                                                                project,
                                                                                                                d.getScope() ) );

            boolean validate = true;

            // Don't bother adding an artifact with a property that couldn't be resolved.
            if (sa.getVersionString().contains( "$" ))
            {
                validate = false;
            }
            // If we are not (re)aligning suffixed dependencies then ignore them.
            // Null check to avoid problems with some tests where state is not instantiated.
            if ( state != null && !state.isRestSuffixAlign() &&
                            ( sa.getVersionString().contains( vs.getRebuildSuffix() ) ||
                                            vs.getSuffixAlternatives().stream().anyMatch( s -> sa.getVersionString().contains( s ) ) ) )
            {
                validate = false;
            }
            if (validate)
            {
                deps.add( sa );
            }
        }
    }


    private static String handlePotentialSnapshotVersion( VersioningState vs, String version )
    {
        if ( vs != null && ! vs.isPreserveSnapshot() )
        {
            return Version.removeSnapshot( version );
        }
        return version;
    }
}
