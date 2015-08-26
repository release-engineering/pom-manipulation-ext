/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.commonjava.maven.ext.manip.impl;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.TypeAndClassifier;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyRESTState;
import org.commonjava.maven.ext.manip.state.State;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.commonjava.maven.ext.manip.rest.DefaultVersionTranslator;
import org.commonjava.maven.ext.manip.rest.VersionTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Wraps DependencyManipulator and VersioningManipulator so we can batch the what-do-we-need-to-change to the REST Client.
 */
@Component( role = Manipulator.class, hint = "dependency-rest-manipulator" )
public class DependencyRESTManipulator
        extends CommonDependencyManipulation
        implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private VersionTranslator restEndpoint;

    private Set<ArtifactRef> localDeps = new HashSet<ArtifactRef>();
    private ProjectVersionRef projVersion;

    private final Map<ProjectVersionRef, String> projectVersionsByGAV = new HashMap<ProjectVersionRef, String>();

    protected DependencyRESTManipulator()
    {
    }

    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        DependencyRESTState state = new DependencyRESTState( userProps );

        restEndpoint = new DefaultVersionTranslator( state.getRESTURL() );

        session.setState( state );
    }

    /**
     * Use the {@link VersionCalculator} to calculate any project version changes, and store them in the {@link VersioningState} that was associated
     * with the {@link ManipulationSession} via the {@link DependencyRESTManipulator#init(ManipulationSession)}
     * method.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
                    throws ManipulationException
    {
        final DependencyRESTState state = session.getState( DependencyRESTState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return;
        }

        // Get the rest endpoint for versiontranslator from the state and initialise it.

        // We need to find the current ProjectVersion and the versions of all Dependencies.

        // Need a better way than current api
        // https://github.com/VaclavDedik/maven-tooling-rest-client/blob/master/src/main/java/org/commonjava/maven/ext/rest/DefaultVersionTranslator.java
        // to return the information.
        //
        // It might be useful to implement https://github.com/release-engineering/pom-manipulation-ext/issues/33 and use ProjectRef everywhere or ProjectVersionRef
        // to avoid Map<String,String> usage.
        //
        // Note that simply returning the new ProjectVersionRefs is not sufficient - as we can't map from old:new (can't guarantee the list is in predictable
        // order for instance).
        // Maybe provide storage within the class and Map<ProjectVersionRef,ProjectVersionRef> for old->new
        //
        // Note current DependencyManipulator loads ProjectRef,NewVersion from the BOM i.e. source for mapping is versionless.

        for ( final Project project : projects )
        {
            if ( project.isInheritanceRoot() )
            {
                projVersion = project.getKey();
            }

            recordDependencies( projects, localDeps, project.getManagedDependencies() );
            recordDependencies( projects, localDeps, project.getDependencies() );

            List<Profile> profiles = project.getModel().getProfiles();
            if ( profiles != null )
            {
                for ( Profile p : profiles )
                {
                    if ( p.getDependencyManagement() != null )
                    {
                        recordDependencies( projects, localDeps, p.getDependencyManagement().getDependencies() );
                    }
                    recordDependencies( projects, localDeps, p.getDependencies() );
                }
            }
        }


        if ( projVersion == null )
        {
            throw new ManipulationException( "Unable to locate inheritance root POM file " );
        }

        // Ok we now have a defined list of top level project plus a unique list of all possible dependencies.
        // Need to send that to the rest interface to get a translation.

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Top level project version is " + projVersion );
            logger.debug( "Dependencies are " + localDeps );
        }

        //### Render into OSGi format.
        //### projVersion = new ProjectVersionRef( projVersion.getGroupId(), projVersion.getArtifactId(),
        //###                                  new Version( projVersion.getVersionString() ).getOSGiVersionString() );

        System.out.println( "Top level project version is " + projVersion );
        System.out.println( "Dependencies are " + localDeps );
    }

    /**
     * Apply any project dependency changes accumulated in the {@link VersioningState} instance associated with the {@link ManipulationSession} to
     * the list of {@link Project}'s given. This happens near the end of the Maven session-bootstrapping sequence, before the projects are
     * discovered/read by the main Maven build initialization.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
                    throws ManipulationException
    {
        final DependencyRESTState state = session.getState( DependencyRESTState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Map<ArtifactRef, String> overrides = new HashMap<ArtifactRef, String>( );
        Map<ProjectVersionRef, String> remote = (Map<ProjectVersionRef, String>) load ( state, session );
        String newProjectVersion = remote.get (projVersion);
        // TODO: handle the resulting projectversion. If its null its the first redhat-x increment. Otherwise we
        // need to use the VersionCalculator to increment and build the next one.
        if (newProjectVersion == null)
        {
            // ### newProjectVersion = new Version( projVersion.getVersionString() ).getOSGiVersionString() );
        }

        // Convert the loaded remote ProjectVersionRefs to the original ArtifactRefs
        for (ArtifactRef a : localDeps)
        {
            if (remote.containsKey( a.asProjectVersionRef() ))
            {
                logger.debug ("### localDeps has " + remote.get(a.asProjectVersionRef()));
                overrides.put( a, remote.get( a.asProjectVersionRef()));
            }
        }
        // Store list of all possible project GAV with new project version
        for (Project p : projects)
        {
            projectVersionsByGAV.put (p.getKey(), newProjectVersion);
        }
        // Call versioning code to convert project versions
        for (Project p : projects)
        {
            // applyVersioningChanges projectVersionsByGAV
        }

        logger.info( "###*** applyChanges for " + this.getClass() );
        Set<Project> changed = internalApplyChanges( projects, session, overrides );
        logger.info( "###*** applyChanges get " + changed );

        return changed;
    }

    @Override
    public Map<? extends ProjectRef, String> load ( final State state, final ManipulationSession session )
            throws ManipulationException
    {
        final Map<ProjectVersionRef, String> result = new HashMap<ProjectVersionRef, String>();
        // ############# SIMULATE REST CALL-------> <---------
        result.put( projVersion, projVersion.getVersionString() );
        for ( ArtifactRef p : localDeps)
        {
            result.put ( p.asProjectVersionRef(), p.getVersionString());
//###                            p.asVersionlessArtifactRef( p.getTypeAndClassifier(), p.isOptional() ), p.getVersionString());
        }
        logger.info ("### Returning result " + result);
        // ################################

        return result;
    }

    @Override
    public int getExecutionIndex()
    {
        return 40;
    }


    /**
     * Translate a given set of dependencies into ProjectVersionRefs.
     *
     * @param projects
     * @param deps Set of ProjectVersionRef to store the results in.
     * @param dependencies dependencies to examine
     */
    private void recordDependencies( List<Project> projects, Set<ArtifactRef> deps, Iterable<Dependency> dependencies )
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
                deps.add( new ArtifactRef( new ProjectVersionRef( d.getGroupId(), d.getArtifactId(),
                                                                  resolveProperties ( projects, d.getVersion())),
//###                                                                  new Version( resolveProperties ( projects, d.getVersion()) ).getOSGiVersionString()),
                                           new TypeAndClassifier( d.getType(), d.getClassifier() ), Boolean.parseBoolean( d.getOptional())));
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
    private String resolveProperties( List<Project> projects, String version )
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
            // TODO: handle the result

            for ( Project p : projects)
            {
                if ( p.getModel().getProperties().containsKey (property) )
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
