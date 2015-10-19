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
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.commonjava.maven.ext.manip.util.IdUtils.gav;

/**
 * {@link Manipulator} implementation that can modify a project's version with either static or calculated, incremental version qualifier. Snapshot
 * versions can be configured to be preserved, though they are truncated to release versions by default. Configuration and accumulated version
 * changes are stored in a {@link VersioningState} instance, which is in turn stored in the {@link ManipulationSession}.
 *
 * This class orchestrates {@link VersionCalculator} scanning/calculation, along with application of the calculated changes at the end of the
 * manipulation process.
 *
 * @author jdcasey
 */
@Component( role = Manipulator.class, hint = "version-manipulator" )
public class ProjectVersioningManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected VersionCalculator calculator;

    protected ProjectVersioningManipulator()
    {
    }

    public ProjectVersioningManipulator( final VersionCalculator calculator )
    {
        this.calculator = calculator;
    }

    /**
     * Use the {@link VersionCalculator} to calculate any project version changes, and store them in the {@link VersioningState} that was associated
     * with the {@link ManipulationSession} via the {@link ProjectVersioningManipulator#init(ManipulationSession)}
     * method.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final VersioningState state = session.getState( VersioningState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( "Version Manipulator: Nothing to do!" );
            return;
        }

        logger.info( "Version Manipulator: Calculating the necessary versioning changes." );
        state.setVersionsByGAVMap( calculator.calculateVersioningChanges( projects, session ) );
    }

    /**
     * Initialize the {@link VersioningState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link ProjectVersioningManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new VersioningState( userProps ) );
    }

    /**
     * Apply any project versioning changes accumulated in the {@link VersioningState} instance associated with the {@link ManipulationSession} to
     * the list of {@link Project}'s given. This happens near the end of the Maven session-bootstrapping sequence, before the projects are
     * discovered/read by the main Maven build initialization.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final VersioningState state = session.getState( VersioningState.class );

        if ( !session.isEnabled() || state == null || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final String ga = ga( project );
            logger.info( getClass().getSimpleName() + " applying changes to: " + ga );
            if ( applyVersioningChanges( project, state ) )
            {
                changed.add( project );
            }
        }

        return changed;
    }

    /**
     * Apply any project versioning changes applicable for the given {@link Model}, using accumulated version-change information stored in the
     * {@link VersioningState} instance, and produced during the {@link ProjectVersioningManipulator#scan(List, ManipulationSession)} invocation.
     *
     * These changes include the main POM version, but may also include the parent declaration and dependencies, if they reference other POMs in the
     * current build.
     *
     * If the project is modified, then it is marked as changed in the {@link ManipulationSession}, which triggers the associated POM to be rewritten.
     *
     * @param project Project undergoing modification.
     * @param state
     * @return whether any changes have been applied.
     * @throws ManipulationException if an error occurs.
     */
    // TODO: Loooong method
    protected boolean applyVersioningChanges( final Project project, VersioningState state )
        throws ManipulationException
    {
        boolean changed = false;

        final Model model = project.getModel();

        if ( !state.hasVersionsByGAVMap() )
        {
            return false;
        }

        if ( model == null )
        {
            return false;
        }

        logger.info( "Looking for applicable versioning changes in: " + gav( model ) );

        String g = model.getGroupId();
        String v = model.getVersion();
        final Parent parent = model.getParent();

        // If the groupId or version is null, it means they must be taken from the parent config
        if ( g == null && parent != null )
        {
            g = parent.getGroupId();
        }
        if ( v == null && parent != null )
        {
            v = parent.getVersion();
        }

        Map<ProjectVersionRef, String> versionsByGAV = state.getVersionsByGAVMap();
        // If the parent version is defined, it might be necessary to change it
        // If the parent version is not defined, it will be taken automatically from the project version
        if ( parent != null && parent.getVersion() != null )
        {
            final ProjectVersionRef parentGAV =
                    new SimpleProjectVersionRef( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
            logger.info( "Looking for parent: " + parentGAV );
            if ( versionsByGAV.containsKey( parentGAV ) )
            {
                final String newVersion = versionsByGAV.get( parentGAV );
                logger.info( "Changed parent version to: " + newVersion + " in " + parent );
                parent.setVersion( newVersion );
                changed = true;
            }
        }

        ProjectVersionRef gav = new SimpleProjectVersionRef( g, model.getArtifactId(), v );
        if ( model.getVersion() != null )
        {
            final String newVersion = versionsByGAV.get( gav );
            logger.info( "Looking for new version: " + gav + " (found: " + newVersion + ")" );
            if ( newVersion != null && model.getVersion() != null )
            {
                model.setVersion( newVersion );
                logger.info( "Changed main version in " + gav( model ) );
                changed = true;
            }
        }
        // If we are at the inheritance root and there is no explicit version instead
        // inheriting the version from the parent BUT the parent is not in this project
        // force inject the new version.
        else if ( changed == false && model.getVersion() == null && project.isInheritanceRoot())
        {
            final String newVersion = versionsByGAV.get( gav );
            logger.info( "Looking to force inject new version for : " + gav + " (found: " + newVersion + ")" );
            if (newVersion != null)
            {
                model.setVersion( newVersion );
                logger.info( "Force inject main version in " + gav( model ) );
                changed = true;
            }
        }

        final Set<ModelBase> bases = new HashSet<ModelBase>();
        bases.add( model );

        final List<Profile> profiles = model.getProfiles();
        if ( profiles != null )
        {
            bases.addAll( profiles );
        }

        final StringSearchInterpolator interp = new StringSearchInterpolator();
        if ( model.getProperties() != null )
        {
            interp.addValueSource( new PropertiesBasedValueSource( model.getProperties() ) );
        }

        final List<String> prefixes = Arrays.asList( "pom", "project" );
        interp.addValueSource( new PrefixedObjectValueSource( prefixes, model, true ) );

        final RecursionInterceptor ri = new PrefixAwareRecursionInterceptor( prefixes, true );

        for ( final ModelBase base : bases )
        {
            final DependencyManagement dm = base.getDependencyManagement();
            if ( dm != null && dm.getDependencies() != null )
            {
                for ( final Dependency d : dm.getDependencies() )
                {
                    try
                    {
                        gav = new SimpleProjectVersionRef( interpolate( d.getGroupId(), ri, interp ),
                                                           interpolate( d.getArtifactId(), ri, interp ),
                                                           interpolate( d.getVersion(), ri, interp ) );
                        final String newVersion = versionsByGAV.get( gav );
                        if ( newVersion != null )
                        {
                            d.setVersion( newVersion );
                            logger.info( "Changed managed: " + d + " in " + base );
                            changed = true;
                        }
                    }
                    catch ( InvalidRefException ire)
                    {
                        logger.debug( "Unable to change version for dependency {} due to {} ", d.toString(), ire );
                    }
                }
            }

            if ( base.getDependencies() != null )
            {
                for ( final Dependency d : base.getDependencies() )
                {
                    try
                    {
                        gav = new SimpleProjectVersionRef( interpolate( d.getGroupId(), ri, interp ),
                                                           interpolate( d.getArtifactId(), ri, interp ),
                                                           interpolate( d.getVersion(), ri, interp ) );

                        final String newVersion = versionsByGAV.get( gav );
                        if ( newVersion != null && d.getVersion() != null )
                        {
                            d.setVersion( newVersion );
                            logger.info( "Changed: " + d + " in " + base );
                            changed = true;
                        }
                    }
                    catch ( InvalidRefException ire)
                    {
                        logger.debug( "Unable to change version for dependency {} due to {} ", d.toString(), ire );
                    }
                }
            }
        }

        if ( changed )
        {
            logger.info( "Applied versioning changes to: " + gav( model ) );
        }

        return changed;
    }

    /**
     * Simple wrapper around the plexus-interpolation call to clean up exception translation in the event of an error.
     */
    private String interpolate( final String src, final RecursionInterceptor ri, final StringSearchInterpolator interp )
        throws ManipulationException
    {
        try
        {
            return interp.interpolate( src, ri );
        }
        catch ( final InterpolationException e )
        {
            throw new ManipulationException( "Failed to interpolate: %s. Reason: %s", e, src, e.getMessage() );
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 20;
    }
}
