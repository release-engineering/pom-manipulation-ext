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

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.CollectionUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.PluginState;
import org.commonjava.maven.ext.manip.state.PluginState.Precedence;
import org.commonjava.maven.ext.manip.state.State;
import org.commonjava.maven.ext.manip.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

/**
 * {@link Manipulator} implementation that can alter plugin sections in a project's pom file.
 * Configuration is stored in a {@link PluginState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "plugin-manipulator" )
public class PluginManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected ModelIO effectiveModelBuilder;

    /**
     * Initialize the {@link PluginState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link Manipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new PluginState( userProps ) );
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
        final PluginState state = session.getState( PluginState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<Project>();

        final Map<ProjectRef, Plugin> overrides = loadRemoteBOM( state, session );

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if (!overrides.isEmpty())
            {
                apply( session, project, model, overrides );

                changed.add( project );
            }
        }
        // If we've changed something now update any old properties with the new values.
        if (!changed.isEmpty())
        {
            logger.info( "Iterating for standard overrides..." );
            for ( final String key : state.getVersionPropertyOverrides().keySet() )
            {
                // Ignore strict alignment for plugins ; if we're attempting to use a differing plugin
                // its unlikely to be an exact match.
                boolean found = PropertiesUtils.updateProperties( session, changed, true, key, state.getVersionPropertyOverride( key ) );

                if ( !found )
                {
                    // Problem in this scenario is that we know we have a property update map but we have not found a
                    // property to update. Its possible this property has been inherited from a parent. Override in the
                    // top pom for safety.
                    logger.info( "Unable to find a property for {} to update", key );
                    for ( final Project p : changed )
                    {
                        if ( p.isInheritanceRoot() )
                        {
                            logger.info( "Adding property {} with {} ", key, state.getVersionPropertyOverride( key ) );
                            p.getModel().getProperties().setProperty( key, state.getVersionPropertyOverride( key ) );
                        }
                    }
                }
            }
        }
        return changed;
    }


    protected Map<ProjectRef, Plugin> loadRemoteBOM( final State state, final ManipulationSession session )
        throws ManipulationException
    {
        final Map<ProjectRef, Plugin> overrides = new LinkedHashMap<ProjectRef, Plugin>();
        final List<ProjectVersionRef> gavs = ( (PluginState) state ).getRemotePluginMgmt();

        if ( gavs == null || gavs.isEmpty() )
        {
            return overrides;
        }

        final ListIterator<ProjectVersionRef> iter = gavs.listIterator( gavs.size() );
        // Iterate in reverse order so that the first GAV in the list overwrites the last

        Properties exclusions = (Properties) session.getUserProperties().clone();
        exclusions.putAll( System.getProperties() );

        while ( iter.hasPrevious() )
        {
            final ProjectVersionRef ref = iter.previous();
            overrides.putAll( effectiveModelBuilder.getRemotePluginVersionOverrides( ref, exclusions ) );
        }

        return overrides;
    }

    protected void apply( final ManipulationSession session, final Project project, final Model model,
                          final Map<ProjectRef, Plugin> override )
        throws ManipulationException
    {
        logger.info( "Applying plugin changes to: " + ga( project ) );

        PluginState state = session.getState( PluginState.class );

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
            applyOverrides( true, pluginManagement.getPlugins(), override, state );
        }

        if ( model.getBuild() != null )
        {
            // Override plugin versions
            final List<Plugin> projectPlugins = model.getBuild()
                                                     .getPlugins();

            // We can't wipe out the versions as we can't guarantee that the plugins are listed
            // in the top level pluginManagement block.
            applyOverrides( false, projectPlugins, override, state );
        }

    }

    /**
     * Set the versions of any plugins which match the contents of the list of plugin overrides
     *
     * @param pluginMgmt Denote whether we are modifying the pluginMgmt block
     * @param plugins The list of plugins to modify
     * @param pluginVersionOverrides The list of version overrides to apply to the plugins
     * @throws ManipulationException if an error occurs.
     */
    protected void applyOverrides( final boolean pluginMgmt, final List<Plugin> plugins,
                                   final Map<ProjectRef, Plugin> pluginVersionOverrides, PluginState pluginState ) throws ManipulationException
    {
        if ( plugins == null)
        {
            throw new ManipulationException ("Original plugins should not be null");
        }

        for ( final Plugin override : pluginVersionOverrides.values())
        {
            final int index = plugins.indexOf( override );
            logger.debug( "plugin override" + override + " and index " + index );

            if ( index != -1 )
            {
                final ProjectRef groupIdArtifactId = new SimpleProjectRef(override.getGroupId(), override.getArtifactId());
                final Plugin plugin = plugins.get( index );

                if ( override.getConfiguration() != null)
                {
                    logger.debug ("Injecting plugin configuration" + override.getConfiguration());
                    if (pluginMgmt && plugin.getConfiguration() == null)
                    {
                        plugin.setConfiguration( override.getConfiguration() );
                        logger.debug( "Altered plugin configuration: " + groupIdArtifactId + "=" + plugin.getConfiguration());
                    }
                    else if (pluginMgmt && plugin.getConfiguration() != null)
                    {
                        logger.debug( "Existing plugin configuration: " + plugin.getConfiguration());

                        if ( ! (plugin.getConfiguration() instanceof Xpp3Dom) || ! (override.getConfiguration() instanceof Xpp3Dom))
                        {
                            throw new ManipulationException ("Incorrect DOM type " + plugin.getConfiguration().getClass().getName() +
                                                             " and" + override.getConfiguration().getClass().getName());
                        }

                        if ( pluginState.getConfigPrecedence() == Precedence.REMOTE)
                        {
                            plugin.setConfiguration ( Xpp3DomUtils.mergeXpp3Dom
                                                      ((Xpp3Dom)override.getConfiguration(), (Xpp3Dom)plugin.getConfiguration() ) );
                        }
                        else if ( pluginState.getConfigPrecedence() == Precedence.LOCAL )
                        {
                            plugin.setConfiguration ( Xpp3DomUtils.mergeXpp3Dom
                                                      ((Xpp3Dom)plugin.getConfiguration(), (Xpp3Dom)override.getConfiguration() ) );
                        }
                        logger.debug( "Altered plugin configuration: " + groupIdArtifactId + "=" + plugin.getConfiguration());
                    }
                }
                else
                {
                    logger.debug ("No remote configuration to inject from " + override.toString());
                }

                if (override.getExecutions() != null)
                {
                    Map<String,PluginExecution> newExecutions = override.getExecutionsAsMap();
                    Map<String,PluginExecution> originalExecutions = plugin.getExecutionsAsMap();

                    for (PluginExecution pe : newExecutions.values())
                    {
                        if (originalExecutions.containsKey( pe.getId() ) )
                        {
                            logger.warn ("Unable to inject execution " + pe.getId() + " as it clashes with an existing execution");
                        }
                        else
                        {
                            logger.debug ("Injecting execution {} ", pe);
                            plugin.getExecutions().add (pe);
                        }
                    }
                }

                if (!override.getDependencies().isEmpty())
                {
                    logger.debug( "Checking original plugin dependencies versus override" );
                    // First, remove any Dependency from the original Plugin if the GA exists in the override.
                    Iterator<Dependency> originalIt = plugin.getDependencies().iterator();
                    while (originalIt.hasNext())
                    {
                        Dependency originalD = originalIt.next();
                        Iterator<Dependency> overrideIt = override.getDependencies().iterator();
                        while ( overrideIt.hasNext() )
                        {
                            Dependency newD = overrideIt.next();
                            if (originalD.getGroupId().equals( newD.getGroupId() ) &&
                                originalD.getArtifactId().equals( newD.getArtifactId() ) )
                            {
                                logger.debug( "Removing original dependency {} in favour of {} ", originalD, newD );
                                originalIt.remove();
                                break;
                            }
                        }
                    }
                    // Now merge them together.
                    logger.debug( "Adding in plugin dependencies {}", override.getDependencies() );
                    plugin.getDependencies().addAll( override.getDependencies() );
                }

                String oldVersion = plugin.getVersion();
                // Always force the version in a pluginMgmt block or set the version if there is an existing
                // one in build/plugins section.
                if ( override.getVersion() != null && !override.getVersion().isEmpty())
                {
                    if ( ! PropertiesUtils.cacheProperty( pluginState.getVersionPropertyOverrides(), oldVersion, override.getVersion(), plugin ))
                    {
                        plugin.setVersion( override.getVersion() );
                        logger.info( "Altered plugin version: " + groupIdArtifactId + "=" + override.getVersion() );
                    }
                }
            }
            // If the plugin doesn't exist but has a configuration section in the remote inject it so we
            // get the correct config.
            else if ( pluginMgmt && ( override.getConfiguration() != null || override.getExecutions() != null ) )
            {
                plugins.add( override );
                logger.info( "Added plugin version: " + override.getKey() + "=" + override.getVersion());
            }
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 60;
    }
}
