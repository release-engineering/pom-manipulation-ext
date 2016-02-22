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
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.PluginState;
import org.commonjava.maven.ext.manip.state.RelocationState;
import org.commonjava.maven.ext.manip.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

/**
 * {@link Manipulator} implementation that can relocation specified groupIds. It will also handle version changes by
 * delegating to dependencyExclusions.
 * Configuration is stored in a {@link PluginState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "relocations-manipulator" )
public class RelocationManipulator
        implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Initialize the {@link PluginState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link Manipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
                    throws ManipulationException
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new RelocationState( userProps ) );
    }

    /**
     * No prescanning required for BOM manipulation.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
            throws ManipulationException
    {
    }

    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
            throws ManipulationException
    {
        final State state = session.getState( RelocationState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if ( apply( session, project, model ) )
            {
                changed.add( project );
            }
        }

        return changed;
    }

    protected boolean apply( final ManipulationSession session, final Project project, final Model model )
            throws ManipulationException
    {
        boolean result = false;
        final RelocationState state = session.getState( RelocationState.class );
        final HashMap<String, ProjectVersionRef> relocations = state.getDependencyRelocations();

        logger.info( "Applying relocation changes to: " + ga( project ) );

        DependencyManagement dependencyManagement = model.getDependencyManagement();
        if ( dependencyManagement != null )
        {
            result = updateDependencies ( session, relocations, dependencyManagement.getDependencies());
        }
        result |= updateDependencies( session, relocations, model.getDependencies() );

        for ( final Profile profile : model.getProfiles() )
        {
            dependencyManagement = profile.getDependencyManagement();
            if ( dependencyManagement != null )
            {
                result |= updateDependencies (session, relocations, dependencyManagement.getDependencies());
            }
            result |= updateDependencies( session, relocations, profile.getDependencies() );

        }
        return result;
    }

    private boolean updateDependencies( ManipulationSession session, HashMap<String, ProjectVersionRef> relocations,
                                        List<Dependency> dependencies )
    {
        boolean result = false;

        for ( final Dependency d : dependencies )
        {
            if (relocations.containsKey( d.getGroupId() ))
            {
                updateDependencyExclusion(session, relocations.get( d.getGroupId() ), d);

                logger.info ("Replacing {} by {}", d.getGroupId(), relocations.get( d.getGroupId() ).getGroupId() );
                d.setGroupId( relocations.get( d.getGroupId() ).getGroupId() );
                result = true;
            }
        }
        return result;
    }

    private void updateDependencyExclusion( ManipulationSession session, ProjectVersionRef projectVersionRef, Dependency d )
    {
        final DependencyState state = session.getState( DependencyState.class );

        if (projectVersionRef.getVersionString().equals( "*" ) )
        {
            logger.debug ("No version alignment to perform for relocations");
        }
        else
        {
            logger.debug ("Adding dependencyExclusion {} & {}", projectVersionRef.getGroupId() + ':' + d.getArtifactId() + "@*",
                          projectVersionRef.getVersionString() );
            state.updateExclusions( projectVersionRef.getGroupId() + ':' + d.getArtifactId() + "@*", projectVersionRef.getVersionString() );
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 15;
    }
}
