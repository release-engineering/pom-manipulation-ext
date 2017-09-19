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
package org.commonjava.maven.ext.manip.impl;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleTypeAndClassifier;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.util.PropertyResolver;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.model.SimpleScopedArtifactRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.RESTState;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.commonjava.maven.ext.manip.util.PropertiesUtils;
import org.commonjava.maven.ext.manip.util.PropertyInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

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
        session.setState( new RESTState( session.getUserProperties() ) );
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
        catch (RestException e)
        {
            throw e;
        }
        finally
        {
            printFinishTime( start, (restResult != null));
        }
        logger.debug ("REST Client returned {} ", restResult);

        vs.setRESTMetadata (parseVersions(session, projects, state, newProjectKeys, restResult));

        final DependencyState ds = session.getState( DependencyState.class );
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
        logger.info ("Added the following ProjectRef:Version from REST call into VersionState {}", versionStates);

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
     * Scans a list of projects and accumulates all non managed dependencies and returns them. Currently only used by the CLI tool to establish
     * a list of non-managed dependencies.
     *
     * @param session
     * @param projects the projects to scan.
     * @param activeProfiles which profiles to check
     * @return an unsorted set of ArtifactRefs used.
     * @throws ManipulationException if an error occurs
     */
    // TODO: Remove
    public static Set<ArtifactRef> establishNonManagedDependencies( ManipulationSession session, final List<Project> projects,
                                                                    Set<String> activeProfiles ) throws ManipulationException
    {
        return establishDependencies( session, projects, activeProfiles, false );
    }


    /**
     * Scans a list of projects and accumulates all dependencies and returns them.
     *
     * @param session
     * @param projects the projects to scan.
     * @param activeProfiles which profiles to check
     * @return an unsorted set of ArtifactRefs used.
     * @throws ManipulationException if an error occurs
     */
    public static Set<ArtifactRef> establishAllDependencies( ManipulationSession session, final List<Project> projects, Set<String> activeProfiles ) throws ManipulationException
    {
        return establishDependencies( session, projects, activeProfiles, true );
    }


    private static Set<ArtifactRef> establishDependencies( ManipulationSession session, final List<Project> projects, Set<String> activeProfiles,
                                                           boolean includeManaged ) throws ManipulationException
    {
        Set<ArtifactRef> localDeps = new TreeSet<>();
        Set<String> activeModules = new HashSet<>();
        boolean scanAll = false;

        if (activeProfiles != null && !activeProfiles.isEmpty())
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
                            if (activeProfiles.contains( p.getId() ) )
                            {
                                logger.debug ("Adding modules for profile {}", p.getId());
                                activeModules.addAll( p.getModules() );
                            }
                        }
                    }
                }
            }
            logger.debug ("Found {} active modules with {} active profiles.", activeModules, activeProfiles);
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
                    SimpleProjectVersionRef parent = new SimpleProjectVersionRef(
                                    project.getModelParent().getGroupId(), project.getModelParent().getArtifactId(), project.getModelParent().getVersion() );
                    localDeps.add( new SimpleArtifactRef(parent, new SimpleTypeAndClassifier( "pom", null )
                    ) );
                }

                if (includeManaged)
                {
                    //TODO: Still to fix this....
                     recordDependencies( session, projects, project, localDeps, project.getResolvedManagedDependencies( session), includeManaged );
                }
                recordDependencies( session, projects, project, localDeps, project.getResolvedDependencies(session), includeManaged );

                List<Profile> profiles = project.getModel().getProfiles();
                if ( profiles != null )
                {
                    for ( Profile p : profiles )
                    {
                        if ( !scanAll && !activeProfiles.contains( p.getId() ) )
                        {
                            continue;
                        }
                        if ( p.getDependencyManagement() != null && includeManaged )
                        {
                            recordDependencies( session, projects, project, localDeps, project.getResolvedProfileManagedDependencies( session ).get (p),
                                                includeManaged );
                        }
                        recordDependencies( session, projects, project, localDeps, project.getResolvedProfileDependencies( session ).get( p ), includeManaged );
                    }
                }
            }

        }

        return localDeps;
    }


    /**
     * Translate a given set of dependencies into ProjectVersionRefs.
     * @param session
     * @param projects list of all projects
     * @param project currently scanned project
     * @param deps Set of ProjectVersionRef to store the results in.
     * @param dependencies dependencies to examine
     * @param excludeEmptyVersions if true, exclude empty versions
     */
    private static void recordDependencies( ManipulationSession session, List<Project> projects, Project project, Set<ArtifactRef> deps,
                                            HashMap<ProjectVersionRef, Dependency> dependencies, boolean excludeEmptyVersions )
    // TODO: called by establishNonManaged with false which is called by the CLI::printUnusedDepMgmt
    // TODO: called by establishAllDeps with true which is called by CLI and RESTManip.
                    throws ManipulationException
    {
        if ( dependencies == null )
        {
            return;
        }

        for ( ProjectVersionRef pvr : dependencies.keySet() )
        {
            Dependency d = dependencies.get( pvr );
            deps.add( new SimpleScopedArtifactRef( pvr, new SimpleTypeAndClassifier( d.getType(), d.getClassifier() ),
                                                   // TODO: Should atlas handle default scope?
                                                   isEmpty( d.getScope() ) ?
                                                                   DependencyScope.compile.realName() :
                                                                   PropertyResolver.resolveInheritedProperties( session,
                                                                                                                project,
                                                                                                                d.getScope() ) ) );
        }

/*
        Iterator<Dependency> iterator = dependencies.keySet().iterator();

        while ( iterator.hasNext() )
        {
            Dependency d = iterator.next();

            if ( excludeEmptyVersions && isEmpty( d.getVersion() ) )
            {
                logger.trace( "Skipping dependency " + d + " as empty version." );
            }
            else
            {
                // TODO: Process hierarchy better to handle a->b being different to a->c->d.
                PropertyInterpolator pi = new PropertyInterpolator( project.getModel().getProperties(), project );
                String version = PropertyResolver.resolveInheritedProperties( session, project, d.getVersion() );
                String groupId = d.getGroupId();
                                //PropertyResolver.resolveInheritedProperties ( session, project, d.getGroupId().equals( "${project.groupId}" ) ? project.getGroupId() : d.getGroupId() );
                String artifactId = d.getArtifactId();
                                //PropertyResolver.resolveInheritedProperties( session, project, d.getArtifactId().equals( "${project.artifactId}" ) ? project.getArtifactId() : d.getArtifactId() );

                if ( isEmpty ( version ) )
                {
                    // TODO: HOW???
                    // Hack for non-managed versions to be stored in this set. Only used by CLI codepath
                    logger.trace( "Version for " + d + " is empty." );
                    version = "<unknown>";
                }

                if ( isNotEmpty( groupId ) && isNotEmpty( artifactId ) )
                {
                    deps.add( new SimpleScopedArtifactRef( new SimpleProjectVersionRef( groupId, artifactId, version ),
                                                           new SimpleTypeAndClassifier( d.getType(), d.getClassifier() ),
                                                           // TODO: Should atlas handle default scope?
                                                           d.getScope() == null ? DependencyScope.compile.realName() : pi.interp( d.getScope() )));
                }
                else
                {
                    logger.warn( "Skipping dependency {} [ {}:{}:{} ]", d, groupId, artifactId, version );
                }
            }
        }
        */
    }


    private void printFinishTime( long start, boolean finished )
    {
        long finish = System.nanoTime();
        long minutes = TimeUnit.NANOSECONDS.toMinutes( finish - start );
        long seconds = TimeUnit.NANOSECONDS.toSeconds( finish - start ) - ( minutes * 60 );
        logger.info ( "REST client finished {}... (took {} min, {} sec, {} millisec)",
                      (finished == true ? "successfully" : "with failures"), minutes, seconds,
                      (TimeUnit.NANOSECONDS.toMillis( finish - start ) - ( minutes * 60 * 1000 ) - ( seconds * 1000) ));
    }
}
