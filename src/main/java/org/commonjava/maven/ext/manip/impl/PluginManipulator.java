package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.IdUtils.ga;

import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.BOMState;
import org.commonjava.maven.ext.manip.state.ManipulationSession;


/**
 * {@link Manipulator} implementation that can alter plugin sections in a project's pom file.
 * Configuration is stored in a {@link BOMState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "plugin-manipulator" )
public class PluginManipulator
    extends AlignmentManipulator
{
    @Requirement
    protected Logger logger;

    protected PluginManipulator()
    {
    }

    public PluginManipulator( final Logger logger )
    {
        super (logger);
        this.logger = logger;
    }

    @Override
    public void init( final ManipulationSession session )
    {
        super.init ( session );
        super.baseLogger = this.logger;
    }

    @Override
    protected Map<String, String> loadRemoteBOM (BOMState state)
        throws ManipulationException
    {
        return loadRemoteOverrides( RemoteType.PLUGIN, state.getRemotePluginMgmt() );
    }

    @Override
    protected void apply (ManipulationSession session, MavenProject project, Model model, Map<String, String> override) throws ManipulationException
    {
        // TODO: Should plugin override apply to all projects?
        logger.info( "Applying plugin changes to: " + ga(project) );

        // If the model doesn't have any plugin management set by default, create one for it
        Build build = model.getBuild();

        if ( build == null )
        {
            build = new Build();
            model.setBuild( build );
            logger.info( "Created new Build for model " + model.getId() );
        }

        PluginManagement pluginManagement = model.getBuild().getPluginManagement();

        if ( pluginManagement == null )
        {
            pluginManagement = new PluginManagement();
            model.getBuild().setPluginManagement( pluginManagement );
            logger.info( "Created new Plugin Management for model " + model.getId() );
        }

        // Override plugin management versions
        applyOverrides( pluginManagement.getPlugins(), override );

        // Override plugin versions
        final List<Plugin> projectPlugins = model.getBuild()
                                                 .getPlugins();
        applyOverrides( projectPlugins, override );

    }

    /**
     * Set the versions of any plugins which match the contents of the list of plugin overrides
     *
     * @param plugins The list of plugins to modify
     * @param pluginVersionOverrides The list of version overrides to apply to the plugins
     */
    protected void applyOverrides( final List<Plugin> plugins, final Map<String, String> pluginVersionOverrides )
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
