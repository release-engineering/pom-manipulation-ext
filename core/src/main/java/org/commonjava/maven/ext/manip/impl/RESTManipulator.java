/**
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
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleTypeAndClassifier;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.model.SimpleScopedArtifactRef;
import org.commonjava.maven.ext.manip.rest.DefaultVersionTranslator;
import org.commonjava.maven.ext.manip.rest.VersionTranslator;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.RESTState;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * This Manipulator runs first. It makes a REST call to an external service to loadRemoteOverrides the GAVs to align the project version
 * and dependencies to. It will prepopulate Project GA versions into the VersioningState in case the VersioningManipulator has been activated
 * and the remote overrides into the DependencyState for those as well.
 */
@Component( role = Manipulator.class, hint = "rest-manipulator" )
public class RESTManipulator implements Manipulator
{
    private static final Logger logger = LoggerFactory.getLogger( RESTManipulator.class );

    private VersionTranslator restEndpoint;

    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        RESTState state = new RESTState( userProps );
        session.setState( state );

        restEndpoint = new DefaultVersionTranslator( state.getRESTURL() );
    }

    /**
     * Prescans the Project to build up a list of Project GAs and also the various Dependencies.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
                    throws ManipulationException
    {
        final RESTState state = session.getState( RESTState.class );
        final ArrayList<ProjectVersionRef> restParam = new ArrayList<ProjectVersionRef>();

        Map<ProjectVersionRef, String> restResult;

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return;
        }

        for ( final Project project : projects )
        {
            // TODO: Check this : For the rest API I think we need to check every project GA not just inheritance root.
            restParam.add( project.getKey() );
        }

        Set<ArtifactRef> localDeps = establishDependencies( projects, null );

        // Ok we now have a defined list of top level project plus a unique list of all possible dependencies.
        // Need to send that to the rest interface to get a translation.

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Project GA and Dependencies are " + localDeps );
        }

        // Call the REST to populate the result.
        for ( ArtifactRef p : localDeps )
        {
            restParam.add( p.asProjectVersionRef() );
        }

        logger.debug ("Calling REST client api with {} ", restParam);
        restResult = restEndpoint.translateVersions( restParam );
        logger.debug ("REST Client returned {} ", restResult);

        // Parse the rest result for the project GAs and store them in versioning state for use
        // there by incremental suffix calculation.
        Map<ProjectRef, Set<String>> versionStates = new HashMap<ProjectRef, Set<String>>();
        for ( final Project p : projects )
        {
            if ( restResult.containsKey( p.getKey() ) )
            {
                // Found part of the current project to store in Versioning State
                Set<String> versions = versionStates.get( p.getKey().asProjectRef() );
                if (versions == null)
                {
                    versions = new HashSet<String>();
                    versionStates.put( p.getKey().asProjectRef(), versions );
                }
                versions.add( restResult.get( p.getKey() ) );
            }
        }
        logger.debug ("Added the following ProjectRef:Version into VersionState" + versionStates);
        final VersioningState vs = session.getState( VersioningState.class );
        vs.setRESTMetadata (versionStates);

        final DependencyState ds = session.getState( DependencyState.class );
        final Map<ArtifactRef, String> overrides = new HashMap<ArtifactRef, String>( );

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
     * No-op in this case - any changes, if configured, would happen in Versioning or Dependency Manipulators.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
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
     * @param projects the projects to scan.
     * @param activeProfiles
     * @return an unsorted set of ArtifactRefs used.
     * @throws ManipulationException
     */
    public static Set<ArtifactRef> establishDependencies( final List<Project> projects, Set<String> activeProfiles ) throws ManipulationException
    {
        Set<ArtifactRef> localDeps = new TreeSet<ArtifactRef>();
        Set<String> activeModules = new HashSet<String>();
        boolean scanAll = false;

        if (activeProfiles != null && activeProfiles.size() > 0)
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
                            if ( activeProfiles != null && activeProfiles.size() > 0 && activeProfiles.contains(p.getId() ) )
                            {
                                activeModules.addAll( p.getModules() );
                            }
                        }
                    }
                }
            }
            logger.info ("Found {} active modules with {} active profiles.", activeModules, activeProfiles);
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
                recordDependencies( projects, localDeps, project.getManagedDependencies() );
                recordDependencies( projects, localDeps, project.getDependencies() );

                List<Profile> profiles = project.getModel().getProfiles();
                if ( profiles != null )
                {
                    for ( Profile p : profiles )
                    {
                        if ( !scanAll && !activeProfiles.contains( p.getId() ) )
                        {
                            continue;
                        }
                        if ( p.getDependencyManagement() != null )
                        {
                            recordDependencies( projects, localDeps, p.getDependencyManagement().getDependencies() );
                        }
                        recordDependencies( projects, localDeps, p.getDependencies() );
                    }
                }
            }

        }

        return localDeps;
    }


    /**
     * Translate a given set of dependencies into ProjectVersionRefs.
     *
     * @param projects
     * @param deps Set of ProjectVersionRef to store the results in.
     * @param dependencies dependencies to examine
     */
    private static void recordDependencies( List<Project> projects, Set<ArtifactRef> deps, Iterable<Dependency> dependencies )
                    throws ManipulationException
    {
        if ( dependencies == null )
        {
            return;
        }

        Iterator<Dependency> iterator = dependencies.iterator();

        while ( iterator.hasNext() )
        {
            Dependency d = iterator.next();

            if ( d.getVersion() == null )
            {
                logger.debug( "Skipping dependency " + d + " as empty version." );
            }
            else
            {
                deps.add( new SimpleScopedArtifactRef( new SimpleProjectVersionRef( d.getGroupId(), d.getArtifactId(),
                                                                                    resolveProperties( projects, d.getVersion() ) ),
                                                       new SimpleTypeAndClassifier( d.getType(), d.getClassifier() ), Boolean.parseBoolean( d.getOptional()),
                                                       // TODO: Should atlas handle default scope?
                                                       d.getScope() == null ? DependencyScope.compile.realName() : d.getScope()));
            }
        }
    }

    /**
     * This recursively checks the supplied version and recursively resolves it if its a property.
     *
     * @param projects set of projects
     * @param version version to check
     * @return the version string
     * @throws ManipulationException
     */
    private static String resolveProperties( List<Project> projects, String version )
                    throws ManipulationException
    {
        String result = version;

        if (version.startsWith( "${" ) )
        {
            final int endIndex = version.indexOf( '}' );
            final String property = version.substring( 2, endIndex );

            if ( endIndex != version.length() - 1 )
            {
                throw new ManipulationException( "NYI : handling for versions (" + version
                                                                 + ") with multiple embedded properties is NYI. " );
            }
            for ( Project p : projects)
            {
                logger.debug( "Scanning {} for property {} and found {} ", p, property, p.getModel().getProperties() );
                if ( property.equals( "project.version" ))
                {
                    result = p.getVersion();
                }
                else if ( p.getModel().getProperties().containsKey (property) )
                {
                    result = p.getModel().getProperties().getProperty( property );

                    if ( result.startsWith( "${" ))
                    {
                        result = resolveProperties( projects, result );
                    }
                }
            }
        }
        return result;
    }
}
