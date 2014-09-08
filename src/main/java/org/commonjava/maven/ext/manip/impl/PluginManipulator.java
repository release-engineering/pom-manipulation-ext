/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.PluginState;
import org.commonjava.maven.ext.manip.state.State;

/**
 * {@link Manipulator} implementation that can alter plugin sections in a project's pom file.
 * Configuration is stored in a {@link PluginState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "plugin-manipulator" )
public class PluginManipulator
    extends AlignmentManipulator
{
    protected PluginManipulator()
    {
    }

    public PluginManipulator( final ModelIO modelIO )
    {
        super( modelIO );
    }

    /**
     * Initialize the {@link PluginState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link AlignmentManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new PluginState( userProps ) );
    }



    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        return internalApplyChanges (session.getState( PluginState.class ), projects, session);
    }

    @Override
    protected Map<ProjectRef, String> loadRemoteBOM( final State state, final ManipulationSession session )
        throws ManipulationException
    {
        return loadRemoteOverrides( RemoteType.PLUGIN, ( (PluginState) state ).getRemotePluginMgmt(), session );
    }

    @Override
    protected void apply( final ManipulationSession session, final Project project, final Model model,
                          final Map<ProjectRef, String> override )
        throws ManipulationException
    {
        // TODO: Should plugin override apply to all projects?
        logger.info( "Applying plugin changes to: " + ga( project ) );

        if ( project.isInheritanceRoot() )
        {
            // If the model doesn't have any plugin management set by default, create one for it
            Build build = model.getBuild();

            if ( build == null )
            {
                build = new Build();
                model.setBuild( build );
                logger.info( "Created new Build for model " + model.getId() );
            }

            PluginManagement pluginManagement = model.getBuild()
                                                     .getPluginManagement();

            if ( pluginManagement == null )
            {
                pluginManagement = new PluginManagement();
                model.getBuild()
                     .setPluginManagement( pluginManagement );
                logger.info( "Created new Plugin Management for model " + model.getId() );
            }

            // Override plugin management versions
            applyOverrides( pluginManagement.getPlugins(), override );
        }

        if ( model.getBuild() != null )
        {
            // Override plugin versions
            final List<Plugin> projectPlugins = model.getBuild()
                                                     .getPlugins();
            applyOverrides( projectPlugins, override );
        }

    }

    /**
     * Set the versions of any plugins which match the contents of the list of plugin overrides
     *
     * @param plugins The list of plugins to modify
     * @param pluginVersionOverrides The list of version overrides to apply to the plugins
     */
    protected void applyOverrides( final List<Plugin> plugins, final Map<ProjectRef, String> pluginVersionOverrides )
    {
        for ( final Plugin plugin : ( plugins == null ? Collections.<Plugin> emptyList() : plugins ) )
        {
            final ProjectRef groupIdArtifactId = new ProjectRef(plugin.getGroupId(), plugin.getArtifactId());
            if ( pluginVersionOverrides.containsKey( groupIdArtifactId ) )
            {
                final String overrideVersion = pluginVersionOverrides.get( groupIdArtifactId );
                plugin.setVersion( overrideVersion );
                logger.info( "Altered plugin: " + groupIdArtifactId + "=" + overrideVersion );
            }
        }
    }
}
