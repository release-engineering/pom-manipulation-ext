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

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.ProfileUtils;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.PluginRemovalState;
import org.commonjava.maven.ext.core.state.PluginState;
import org.commonjava.maven.ext.core.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.commonjava.maven.ext.core.util.IdUtils.ga;

/**
 * {@link Manipulator} implementation that can remove specified plugins from a project's pom file.
 * Configuration is stored in a {@link PluginState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Named("plugin-removal-manipulator")
@Singleton
public class PluginRemovalManipulator
        implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    /**
     * Initialize the {@link PluginState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        this.session = session;
        session.setState( new PluginRemovalState( session.getUserProperties() ) );
    }

    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
    {
        final State state = session.getState( PluginRemovalState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if ( apply( project, model ) )
            {
                changed.add( project );
            }
        }

        return changed;
    }

    private boolean apply( final Project project, final Model model )
    {
        final PluginRemovalState state = session.getState( PluginRemovalState.class );

        logger.debug( "Applying plugin changes to: " + ga( project ) );

        boolean result = false;
        List<ProjectRef> pluginsToRemove = state.getPluginRemoval();
        if ( model.getBuild() != null )
        {
            result = scanPlugins( pluginsToRemove, model.getBuild().getPlugins() );
        }

        for ( final Profile profile : ProfileUtils.getProfiles( session, model) )
        {
            if ( profile.getBuild() != null && scanPlugins( pluginsToRemove, profile.getBuild().getPlugins() ) )
            {
                result = true;
            }
        }
        return result;
    }

    private boolean scanPlugins( List<ProjectRef> pluginsToRemove, List<Plugin> plugins )
    {
        boolean result = false;
        if ( plugins != null )
        {
            Iterator<Plugin> it = plugins.iterator();
            while ( it.hasNext() )
            {
                Plugin p = it.next();
                if ( pluginsToRemove.contains( SimpleProjectRef.parse( p.getKey() ) ) )
                {
                    logger.debug( "Removing {} ", p.toString() );
                    it.remove();
                    result = true;
                }
            }
        }
        return result;
    }

    @Override
    public int getExecutionIndex()
    {
        return 52;
    }
}
