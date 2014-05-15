package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.IdUtils.ga;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.IdUtils;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.resolver.EffectiveModelBuilder;
import org.commonjava.maven.ext.manip.state.BOMState;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

/**
 * {@link Manipulator} implementation that can alter property and dependency management sections in a project's pom file.
 * Configuration is stored in a {@link BOMState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "bom-manipulator" )
public class BOMManipulator
    implements Manipulator
{
    @Requirement
    protected Logger logger;

    private enum RemoteType
    {
        PROPERTY, PLUGIN
    };

    protected BOMManipulator()
    {
    }

    public BOMManipulator( final Logger logger )
    {
        this.logger = logger;
    }

    /**
     * No prescanning required for BOM manipulation.
     */
    @Override
    public void scan( final List<MavenProject> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link BOMState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link BOMManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new BOMState( userProps ) );
    }

    /**
     * Apply the reporting and repository removal changes to the list of {@link MavenProject}'s given.
     * This happens near the end of the Maven session-bootstrapping sequence, before the projects are
     * discovered/read by the main Maven build initialization.
     */
    @Override
    public Set<MavenProject> applyChanges( final List<MavenProject> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final BOMState state = session.getState( BOMState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.info( "Version Manipulator: Nothing to do!" );
            return Collections.emptySet();
        }

        final Map<String, Model> manipulatedModels = session.getManipulatedModels();
        final Map<String, String> propertyOverride = loadRemoteOverrides( RemoteType.PROPERTY, state.getRemotePropertyMgmt() );
        final Map<String, String> pluginOverride = loadRemoteOverrides( RemoteType.PLUGIN, state.getRemotePluginMgmt() );
        final Set<MavenProject> changed = new HashSet<MavenProject>();

        for ( final MavenProject project : projects )
        {
            if ( propertyOverride.size() > 0 && project.isExecutionRoot() )
            {
                final String ga = ga( project );
                logger.info( "Applying property changes to: " + ga );
                final Model model = manipulatedModels.get( ga );

                model.getProperties()
                     .putAll( propertyOverride );
                changed.add( project );
            }
            if ( pluginOverride.size() > 0 && project.isExecutionRoot() )
            {
                final String ga = ga( project );
                logger.info( "Applying plugin changes to: " + ga );
                final Model model = manipulatedModels.get( ga );

                // If the model doesn't have any plugin management set by default, create one for it
                PluginManagement pluginManagement = model.getBuild()
                                                         .getPluginManagement();

                if ( pluginManagement == null )
                {
                    pluginManagement = new PluginManagement();
                    model.getBuild()
                         .setPluginManagement( pluginManagement );
                    logger.info( "Created new Plugin Management for model" );
                }

                // Override plugin management versions
                applyOverrides( pluginManagement.getPlugins(), pluginOverride );

                // Override plugin versions
                final List<Plugin> projectPlugins = model.getBuild()
                                                         .getPlugins();
                applyOverrides( projectPlugins, pluginOverride );

                changed.add( project );
            }
        }

        return changed;
    }

    /**
     * Get property mappings from a remote POM
     *
     * @return Map between the GA of the plugin and the version of the plugin. If the system property is not set,
     *         returns an empty map.
     */
    private Map<String, String> loadRemoteOverrides( final RemoteType rt, final String remoteMgmt )
        throws ManipulationException
    {
        final Map<String, String> overrides = new HashMap<String, String>();

        if ( remoteMgmt == null || remoteMgmt.length() == 0 )
        {
            return overrides;
        }

        final String[] remoteMgmtPomGAVs = remoteMgmt.split( "," );

        // Iterate in reverse order so that the first GAV in the list overwrites the last
        for ( int i = ( remoteMgmtPomGAVs.length - 1 ); i > -1; --i )
        {
            final String nextGAV = remoteMgmtPomGAVs[i];

            if ( !IdUtils.validGav( nextGAV ) )
            {
                logger.warn( "Skipping invalid remote management GAV: " + nextGAV );
                continue;
            }
            switch ( rt )
            {
                case PROPERTY:
                    overrides.putAll( EffectiveModelBuilder.getInstance()
                                                           .getRemotePropertyMappingOverrides( nextGAV ) );
                    break;
                case PLUGIN:
                    overrides.putAll( EffectiveModelBuilder.getInstance()
                                                           .getRemotePluginVersionOverrides( nextGAV ) );
                    break;
            }
        }
        logger.info( "### remote override loaded" + overrides );

        return overrides;
    }

    /**
     * Set the versions of any plugins which match the contents of the list of plugin overrides
     *
     * @param plugins The list of plugins to modify
     * @param pluginVersionOverrides The list of version overrides to apply to the plugins
     */
    private void applyOverrides( final List<Plugin> plugins, final Map<String, String> pluginVersionOverrides )
    {
        for ( final Plugin plugin : plugins )
        {
            final String groupIdArtifactId = plugin.getGroupId() + BOMState.GAV_SEPERATOR + plugin.getArtifactId();
            if ( pluginVersionOverrides.containsKey( groupIdArtifactId ) )
            {
                final String overrideVersion = pluginVersionOverrides.get( groupIdArtifactId );
                plugin.setVersion( overrideVersion );
                logger.info( "Altered plugin: " + groupIdArtifactId + "=" + overrideVersion );
            }
        }
    }
}
