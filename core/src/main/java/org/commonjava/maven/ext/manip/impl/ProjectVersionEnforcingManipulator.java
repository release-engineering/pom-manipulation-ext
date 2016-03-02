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
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ProjectVersionEnforcingState;
import org.commonjava.maven.ext.manip.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link Manipulator} implementation that looks for POMs that use ${project.version} rather than
 * an explicit version.
 *
 * Importing such a POM is not a problem, but if the POM is inherited then it will immediately break the
 * build as the ${project.version} is resolved to the current project not the version of the POM.
 *
 * Therefore this manipulator will automatically fix these unless it is explicitly disabled.
 */
@Component( role = Manipulator.class, hint = "enforce-project-version" )
public class ProjectVersionEnforcingManipulator
    implements Manipulator
{

    private static final String PROJVER = "${project.version}";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected ProjectVersionEnforcingManipulator()
    {
    }

    /**
     * Sets the mode based on user properties and defaults.
     * @see ProjectVersionEnforcingState
     */
    @Override
    public void init( final ManipulationSession session )
    {
        session.setState( new ProjectVersionEnforcingState( session.getUserProperties() ) );
    }

    /**
     * No pre-scanning necessary.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * For each project in the current build set, reset the version if using project.version
    */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final ProjectVersionEnforcingState state = session.getState( ProjectVersionEnforcingState.class );
        if ( !session.isEnabled() ||
                        !session.anyStateEnabled( State.activeByDefault ) ||
                        state == null || !state.isEnabled() )
        {
            logger.debug( "Project version enforcement is disabled." );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if ( model.getPackaging().equals( "pom" ) )
            {
                enforceProjectVersion( project, model.getDependencies(), changed );

                if ( model.getDependencyManagement() != null )
                {
                    enforceProjectVersion( project, model.getDependencyManagement().getDependencies(), changed );
                }

                final List<Profile> profiles = model.getProfiles();
                if ( profiles != null )
                {
                    for ( final Profile profile : model.getProfiles() )
                    {
                        enforceProjectVersion( project, profile.getDependencies(), changed );
                        if ( profile.getDependencyManagement() != null )
                        {
                            enforceProjectVersion( project, profile.getDependencyManagement().getDependencies(),
                                                   changed );
                        }
                    }
                }
            }
        }
        if ( changed.size() > 0 )
        {
            logger.warn( "Using ${project.version} in pom files may lead to unexpected errors with inheritance." );
        }
        return changed;
    }

    private void enforceProjectVersion( final Project project, final List<Dependency> dependencies, final Set<Project> changed )
    {
        for ( final Dependency d : dependencies)
        {
            if ( d.getVersion() != null && d.getVersion().contains( PROJVER ) )
            {
                String newVersion =  project.getModel().getVersion() == null ?
                                project.getModel().getParent ().getVersion () : project.getModel().getVersion ();

                logger.debug( "Replacing project.version within {} for project {} with {}", d, project, newVersion);

                d.setVersion( newVersion );
                changed.add( project );
            }
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 55;
    }
}
