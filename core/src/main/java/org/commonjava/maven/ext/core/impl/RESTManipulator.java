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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.atlas.ident.DependencyScope;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * This Manipulator runs first. It makes a REST call to an external service to loadRemoteOverrides the GAVs to align the project version
 * and dependencies to. It will prepopulate Project GA versions into the VersioningState in case the VersioningManipulator has been activated
 * and the remote overrides into the DependencyState for those as well.
 */
@Component( role = Manipulator.class, hint = "rest-manipulator" )
public class RESTManipulator implements Manipulator
{
    private static final Logger logger = LoggerFactory.getLogger( RESTManipulator.class );

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
    @Override
    public void scan( final List<Project> projects )
                    throws ManipulationException
    {
        final RESTState state = session.getState( RESTState.class );
        final VersioningState vs = session.getState( VersioningState.class );
        final DependencyState ds = session.getState( DependencyState.class );
        final PluginState ps = session.getState( PluginState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return;
        }

        final ArrayList<ProjectVersionRef> restParam = new ArrayList<>();
        final ArrayList<ProjectVersionRef> newProjectKeys = new ArrayList<>();

        final String override = vs.getOverride();

        for ( final Project project : projects )
        {
            if ( isEmpty( override ) )
            {
                // TODO: Check this : For the rest API I think we need to check every project GA not just inheritance root.
                // Strip SNAPSHOT from the version for matching. DA will handle OSGi conversion.
                ProjectVersionRef newKey = new SimpleProjectVersionRef( project.getKey() );

                if ( project.getKey().getVersionString().endsWith( "-SNAPSHOT" ) )
                {
                    if ( !vs.preserveSnapshot() )
                    {
                        newKey = new SimpleProjectVersionRef( project.getKey().asProjectRef(), project.getKey()
                                                                                                      .getVersionString()
                                                                                                      .substring( 0,
                                                                                                                  project.getKey()
                                                                                                                         .getVersionString()
                                                                                                                         .indexOf( "-SNAPSHOT" ) ) );
                    }
                    else
                    {
                        logger.warn( "SNAPSHOT detected for REST call but preserve-snapshots is enabled." );
                    }
                }
                newProjectKeys.add( newKey );
            }
            else if ( project.isExecutionRoot() )
            {
                // We want to manually override the version ; therefore ignore what is in the project and calculate potential
                // matches for that instead.
                Project p = projects.get( 0 );
                newProjectKeys.add( new SimpleProjectVersionRef( p.getGroupId(), p.getArtifactId(), override ) );
            }
        }
        restParam.addAll( newProjectKeys );

        // If the dependencyState getRemoteBOMDepMgmt contains suffix then send that to process as well.
        // We only recognise dependencyManagement of the form g:a:version-rebuild not g:a:version-rebuild-<numeric>.
        for ( ProjectVersionRef bom : ( ds.getRemoteBOMDepMgmt() == null ? Collections.<ProjectVersionRef>emptyList() : ds.getRemoteBOMDepMgmt() ) )
        {
            if ( ! Version.hasBuildNumber( bom.getVersionString() ) && bom.getVersionString().contains( PropertiesUtils.getSuffix( session ) ) )
            {
                // Create the dummy PVR to send to DA (which requires a numeric suffix).
                ProjectVersionRef newBom = new SimpleProjectVersionRef( bom.asProjectRef(), bom.getVersionString() + "-0" );
                logger.debug ("Adding dependencyManagement BOM {} into REST call.", newBom);
                restParam.add( newBom );
            }
        }

        Set<ArtifactRef> localDeps = establishAllDependencies( session, projects, null );
        // Ok we now have a defined list of top level project plus a unique list of all possible dependencies.
        // Need to send that to the rest interface to get a translation.
        for ( ArtifactRef p : localDeps )
        {
            restParam.add( p.asProjectVersionRef() );
        }

        // Call the REST to populate the result.
        logger.debug ("Passing {} GAVs following into the REST client api {} ", restParam.size(), restParam);
        logger.info ("Calling REST client...");
        long start = System.nanoTime();
        Map<ProjectVersionRef, String> restResult = null;

        try
        {
            restResult = state.getVersionTranslator().translateVersions( restParam );
        }
        finally
        {
            printFinishTime( start, (restResult != null));
        }
        logger.debug ("REST Client returned {} ", restResult);

        // Process rest result for boms
        ListIterator<ProjectVersionRef> iterator = (ds.getRemoteBOMDepMgmt() == null ? Collections.<ProjectVersionRef>emptyList().listIterator() : ds.getRemoteBOMDepMgmt().listIterator());
        while ( iterator.hasNext() )
        {
            ProjectVersionRef pvr = iterator.next();
            // As before, only process the BOMs if they are of the format <rebuild suffix> without a numeric portion.
            if ( ! Version.hasBuildNumber( pvr.getVersionString() ) && pvr.getVersionString().contains( PropertiesUtils.getSuffix( session ) ) )
            {
                // Create the dummy PVR to compare with results to...
                ProjectVersionRef newBom = new SimpleProjectVersionRef( pvr.asProjectRef(), pvr.getVersionString() + "-0" );
                if ( restResult.keySet().contains( newBom ) )
                {
                    ProjectVersionRef replacementBOM = new SimpleProjectVersionRef( pvr.asProjectRef(), restResult.get( newBom ) );
                    logger.debug( "Replacing BOM value of {} with {}.", pvr, replacementBOM );
                    iterator.remove();
                    iterator.add( replacementBOM );
                }
            }
        }

        vs.setRESTMetadata (parseVersions(session, projects, state, newProjectKeys, restResult));

        final Map<ArtifactRef, String> overrides = new HashMap<>();

        // Convert the loaded remote ProjectVersionRefs to the original ArtifactRefs
        for (ArtifactRef a : localDeps )
        {
            if (restResult.containsKey( a.asProjectVersionRef() ))
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
    private Map<ProjectRef, Set<String>> parseVersions( ManipulationSession session, List<Project> projects, RESTState state, ArrayList<ProjectVersionRef> newProjectKeys,
                                                        Map<ProjectVersionRef, String> restResult )
                    throws ManipulationException
    {
        Map<ProjectRef, Set<String>> versionStates = new HashMap<>();
        for ( final ProjectVersionRef p : newProjectKeys )
        {
            if ( restResult.containsKey( p ) )
            {
                // Found part of the current project to store in Versioning State
                Set<String> versions = versionStates.get( p.asProjectRef() );
                if (versions == null)
                {
                    versions = new HashSet<>();
                    versionStates.put( p.asProjectRef(), versions );
                }
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
                    Set<String> versions = versionStates.get( p.getKey().asProjectRef() );
                    if ( versions == null )
                    {
                        versions = new HashSet<>();
                        versionStates.put( p.getKey().asProjectRef(), versions );
                    }
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
        return Collections.emptySet();
    }

    @Override
    public int getExecutionIndex()
    {
        // Low value index so it runs first in order to call the REST API.
        return 5;
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
        Set<ArtifactRef> localDeps = new TreeSet<>();
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
                                                                                  project.getModelParent().getVersion() );
                    localDeps.add( new SimpleArtifactRef( parent, new SimpleTypeAndClassifier( "pom", null ) ) );
                }

                recordDependencies( session, project, localDeps, project.getResolvedManagedDependencies( session ) );
                recordDependencies( session, project, localDeps, project.getResolvedDependencies( session ) );
                recordPlugins( localDeps, project.getResolvedManagedPlugins( session ) );
                recordPlugins( localDeps, project.getResolvedPlugins( session ) );

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
                                                    project.getResolvedProfileManagedDependencies( session ).get( p ) );
                        recordDependencies( session, project, localDeps,
                                            project.getResolvedProfileDependencies( session ).get( p ) );
                        recordPlugins( localDeps, project.getResolvedProfileManagedPlugins( session ).get( p ) );
                        recordPlugins( localDeps, project.getResolvedProfilePlugins( session ).get( p ) );

                    }
                }
            }
        }
        return localDeps;
    }

    /**
     * Translate a given set of pvr:plugins into ArtifactRefs.
     * @param deps the list of ArtifactRefs to return
     * @param plugins the plugins to transform.
     */
    private static void recordPlugins( Set<ArtifactRef> deps, HashMap<ProjectVersionRef, Plugin> plugins )
    {
        if ( plugins == null )
        {
            return;
        }

        for ( ProjectVersionRef pvr : plugins.keySet() )
        {
            deps.add( new SimpleScopedArtifactRef( pvr, new SimpleTypeAndClassifier( "maven-plugin", null ),
                                                   DependencyScope.compile.realName() ) );
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
                                            HashMap<ArtifactRef, Dependency> dependencies )
                    throws ManipulationException
    {
        if ( dependencies == null )
        {
            return;
        }

        for ( ArtifactRef pvr : dependencies.keySet() )
        {
            Dependency d = dependencies.get( pvr );
            deps.add( new SimpleScopedArtifactRef( pvr, new SimpleTypeAndClassifier( d.getType(), d.getClassifier() ),
                                                   isEmpty( d.getScope() ) ?
                                                                   DependencyScope.compile.realName() :
                                                                   PropertyResolver.resolveInheritedProperties( session,
                                                                                                                project,
                                                                                                                d.getScope() ) ) );
        }
    }


    private void printFinishTime( long start, boolean finished )
    {
        long finish = System.nanoTime();
        long minutes = TimeUnit.NANOSECONDS.toMinutes( finish - start );
        long seconds = TimeUnit.NANOSECONDS.toSeconds( finish - start ) - ( minutes * 60 );
        logger.info ( "REST client finished {}... (took {} min, {} sec, {} millisec)",
                      ( finished ? "successfully" : "with failures"), minutes, seconds,
                      (TimeUnit.NANOSECONDS.toMillis( finish - start ) - ( minutes * 60 * 1000 ) - ( seconds * 1000) ));
    }
}
