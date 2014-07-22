package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.commonjava.maven.ext.manip.util.PropertiesUtils.getPropertiesByPrefix;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.ConfigurationContainer;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.state.DistributionEnforcingState;
import org.commonjava.maven.ext.manip.state.EnforcingMode;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.galley.maven.parse.GalleyMavenXMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@link Manipulator} implementation that looks for the deploy- and install-plugin &lt;skip/&gt; options, and enforces one of a couple scenarios:
 * <ul>
 * <li><code>-Denforce-skip=(on|true)</code> forces them to be set to <code>true</code> (install/deploy will NOT happen)</li>
 * <li><code>-Denforce-skip=(off|false)</code> forces them to be set to <code>false</code> (install/deploy will happen)</li>
 * <li><code>-Denforce-skip=detect</code> forces the deploy- and install-plugin's skip option to be aligned with that of the first detected install 
 *     plugin</li>
 * <li><code>-Denforce-skip=none</code> disables enforcement.</li>
 * </ul>
 * 
 * <br/>
 * 
 * <b>NOTE:</b> When using the <code>detect</code> mode, only the install-plugin configurations in the main pom (<b>not</b> those in profiles) will 
 * be considered for detection. Of these, only parameters in the plugin-wide configuration OR the <code>default-install</code> execution 
 * configuration will be considered. If no matching skip-flag configuration is detected, the default mode of <code>on</code> will be used.
 * 
 * <br/>
 * 
 * Likewise, it's possible to set the enforcement mode DIFFERENTLY for a single project, using:
 * <br/>
 * 
 * <pre>
 * <code>-DenforceSkip.org.group.id:artifact-id=(on|true|off|false|detect|none)</code>
 * </pre>
 * 
 * <br/>
 * 
 * This is for systems that compare the installed artifacts against the 
 * deployed artifacts as part of a post-build validation process.
 * 
 * @author jdcasey
 */
@Component( role = Manipulator.class, hint = "enforce-skip" )
public class DistributionEnforcingManipulator
    implements Manipulator
{

    public static final String MAVEN_PLUGIN_GROUPID = "org.apache.maven.plugins";

    public static final String MAVEN_INSTALL_ARTIFACTID = "maven-install-plugin";

    public static final String MAVEN_DEPLOY_ARTIFACTID = "maven-deploy-plugin";

    public static final String SKIP_NODE = "skip";

    public static final String DEFAULT_INSTALL_EXEC = "default-install";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected GalleyAPIWrapper galleyWrapper;

    protected DistributionEnforcingManipulator()
    {
    }

    public DistributionEnforcingManipulator( final GalleyAPIWrapper galleyWrapper )
    {
        this.galleyWrapper = galleyWrapper;
    }

    /** Sets the mode to on, off, detect (from install plugin), or none (disabled) based on user properties.
     * @see DistributionEnforcingState
     */
    @Override
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        session.setState( new DistributionEnforcingState( session.getUserProperties() ) );
    }

    /** No pre-scanning necessary.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * For each project in the current build set, enforce the value of the plugin-wide skip flag and that of the 'default-deploy' execution, if they
     * exist. There are three possible modes for enforcement:
     * 
     * <ul>
     *   <li><b>on</b> - Ensure install and deploy skip is <b>disabled</b>, and that these functions will happen during the build.</li>
     *   <li><b>off</b> - Ensure install and deploy skip is <b>enabled</b>, and that neither of these functions will happen during the build.</li>
     *   <li><b>detect</b> - Detect the proper flag value from the install plugin's <code>skip</code> flag (either in the plugin-wide config or the 
     *       <code>default-install</code> execution, if it's specified in the main POM, not a profile). If not present, disable the skip flag. 
     *       Enforce consistency with this value install/deploy.</li>
     * </ul>
     * 
     * <b>NOTE:</b> It's possible to specify an enforcement mode that's unique to a single project, using a command-line parameter of: 
     * <code>-DdistroExclusion.g:a=&lt;mode&gt;</code>.
     * 
     * @see DistributionEnforcingState
     * @see EnforcingMode
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final DistributionEnforcingState state = session.getState( DistributionEnforcingState.class );
        if ( state == null || !state.isEnabled() )
        {
            logger.debug( "Distribution skip-flag enforcement is disabled." );
            return Collections.emptySet();
        }

        final Map<String, String> excluded =
            getPropertiesByPrefix( session.getUserProperties(), DistributionEnforcingState.PROJECT_EXCLUSION_PREFIX );

        final Map<String, Model> manipulatedModels = session.getManipulatedModels();
        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final String ga = ga( project );

            EnforcingMode mode = state.getEnforcingMode();

            final String override = excluded.get( ga );
            if ( override != null )
            {
                mode = EnforcingMode.getMode( override );
            }

            if ( mode == EnforcingMode.none )
            {
                logger.info( "Install/Deploy skip-flag enforcement is disabled for: {}.", ga );
                continue;
            }

            logger.info( getClass().getSimpleName() + " applying skip-flag enforment mode of: " + mode + " to: " + ga );

            final Model model = manipulatedModels.get( ga );

            // this is 3-value logic, where skip == on == true, don't-skip == off == false, and (detect from install) == detect == null
            Boolean baseSkipSetting = mode.defaultModificationValue();

            baseSkipSetting = enforceSkipFlag( model, baseSkipSetting, project, changed, true );

            final List<Profile> profiles = model.getProfiles();
            if ( profiles != null )
            {
                for ( final Profile profile : model.getProfiles() )
                {
                    enforceSkipFlag( profile, baseSkipSetting, project, changed, false );
                }
            }
        }

        return changed;
    }

    /**
     * For every mention of a <code>skip</code> parameter in either the install or deploy plugins, enforce a particular value that's passed in. If the
     * passed-in value is <code>null</code> AND the detectFlagValue parameter is true, then look for an install-plugin configuration (in either the
     * plugin-wide config or that of the default-install execution ONLY) that contains the <code>skip</code> flag, and use that as the enforced value.
     * 
     * If detection is enabled and no install-plugin is found, set the value to false (don't skip install or deploy).
     * 
     * Return the detected value, if detection is enabled.
     */
    private Boolean enforceSkipFlag( final ModelBase base, Boolean baseSkipSetting, final Project project,
                                     final Set<Project> changed, final boolean detectFlagValue )
        throws ManipulationException
    {
        // search for install/skip config option, use the first one found...
        Boolean skipSetting = baseSkipSetting;

        List<SkipReference> skipRefs = findSkipRefs( base, MAVEN_INSTALL_ARTIFACTID, project );

        if ( !skipRefs.isEmpty() )
        {
            if ( detectFlagValue && skipSetting == null )
            {
                // we need to set the local value AND the global value.
                final SkipReference ref = skipRefs.get( 0 );
                final ConfigurationContainer container = ref.getContainer();
                if ( !( container instanceof PluginExecution )
                    || ( (PluginExecution) container ).getId()
                                                      .equals( DEFAULT_INSTALL_EXEC ) )
                {
                    String textVal = ref.getNode()
                                        .getTextContent();
                    if ( textVal != null )
                    {
                        textVal = textVal.trim();
                    }

                    if ( textVal.length() > 0 )
                    {
                        skipSetting = Boolean.parseBoolean( textVal );
                        baseSkipSetting = skipSetting;
                    }
                }
            }
        }
        else if ( detectFlagValue && skipSetting == null )
        {
            skipSetting = false;
        }

        if ( skipSetting == null )
        {
            logger.warn( "No setting to enforce for skip-flag! Aborting enforcement..." );
            return null;
        }

        if ( !skipRefs.isEmpty() )
        {
            for ( final SkipReference ref : skipRefs )
            {
                setFlag( ref, skipSetting, project, changed );
            }
        }

        skipRefs = findSkipRefs( base, MAVEN_DEPLOY_ARTIFACTID, project );
        if ( !skipRefs.isEmpty() )
        {
            for ( final SkipReference ref : skipRefs )
            {
                setFlag( ref, skipSetting, project, changed );
            }
        }

        return skipSetting;
    }

    private void setFlag( final SkipReference ref, final Boolean skipSetting, final Project project,
                          final Set<Project> changed )
        throws ManipulationException
    {
        final String old = ref.getNode()
                              .getTextContent()
                              .trim();
        final String nxt = Boolean.toString( skipSetting );
        ref.getNode()
           .setTextContent( nxt );

        ref.getContainer()
           .setConfiguration( getConfigXml( ref.getNode() ) );

        //        logger.info( "Checking for changed POM:\nold skip setting:\n'{}'\n\nNew skip setting:\n'{}'\n", old, nxt );
        if ( !old.equals( nxt ) )
        {
            changed.add( project );
        }
    }

    private Xpp3Dom getConfigXml( final Node node )
        throws ManipulationException
    {
        final String config = galleyWrapper.toXML( node.getOwnerDocument(), false )
                                           .trim();

        try
        {
            return Xpp3DomBuilder.build( new StringReader( config ) );
        }
        catch ( final XmlPullParserException e )
        {
            throw new ManipulationException(
                                             "Failed to re-parse plugin configuration into Xpp3Dom: %s\nConfig was:\n%s",
                                             e, e.getMessage(), config );
        }
        catch ( final IOException e )
        {
            throw new ManipulationException(
                                             "Failed to re-parse plugin configuration into Xpp3Dom: %s\nConfig was:\n%s",
                                             e, e.getMessage(), config );
        }
    }

    /**
     * Go through the plugin / plugin-execution configurations and find references to the <code>skip</code> parameter for the given Maven plugin 
     * (specified by artifactId), both in managed and concrete plugin declarations (where available).
     */
    private List<SkipReference> findSkipRefs( final ModelBase base, final String pluginArtifactId, final Project project )
        throws ManipulationException
    {
        final String key = ga( MAVEN_PLUGIN_GROUPID, pluginArtifactId );

        final List<SkipReference> result = new ArrayList<SkipReference>();

        Map<String, Plugin> pluginMap = project.getManagedPluginMap( base );
        Plugin plugin = pluginMap.get( key );
        result.addAll( findSkipRefs( plugin, project ) );

        pluginMap = project.getPluginMap( base );
        plugin = pluginMap.get( key );
        result.addAll( findSkipRefs( plugin, project ) );

        return result;
    }

    /**
     * Go through the plugin / plugin-execution configurations and find references to the <code>skip</code> parameter for the given Maven plugin 
     * instance.
     */
    private List<SkipReference> findSkipRefs( final Plugin plugin, final Project project )
        throws ManipulationException
    {
        if ( plugin == null )
        {
            return Collections.emptyList();
        }

        final Map<ConfigurationContainer, String> configs = new LinkedHashMap<ConfigurationContainer, String>();
        Object configuration = plugin.getConfiguration();
        if ( configuration != null )
        {
            configs.put( plugin, configuration.toString() );
        }

        final List<PluginExecution> executions = plugin.getExecutions();
        if ( executions != null )
        {
            for ( final PluginExecution execution : executions )
            {
                configuration = execution.getConfiguration();
                if ( configuration != null )
                {
                    configs.put( execution, configuration.toString() );
                }
            }
        }

        final List<SkipReference> result = new ArrayList<SkipReference>();
        for ( final Map.Entry<ConfigurationContainer, String> entry : configs.entrySet() )
        {
            try
            {
                final Document doc = galleyWrapper.parseXml( entry.getValue() );
                final NodeList children = doc.getDocumentElement()
                                             .getChildNodes();
                if ( children != null )
                {
                    for ( int i = 0; i < children.getLength(); i++ )
                    {
                        final Node n = children.item( i );
                        if ( n.getNodeName()
                              .equals( SKIP_NODE ) )
                        {
                            result.add( new SkipReference( entry.getKey(), n ) );
                        }
                    }
                }
            }
            catch ( final GalleyMavenXMLException e )
            {
                throw new ManipulationException( "Unable to parse config for plugin: %s in: %s", e, plugin.getId(),
                                                 project.getId() );
            }
        }

        return result;
    }

    /**
     * store the tuple {container, node} where container is the plugin or plugin execution and node is the skip configuration parameter. 
     * This allows modification of the Model or extraction of the flag value (if we're trying to detect the install plugin's skip flag state).
     */
    private static final class SkipReference
    {
        private final ConfigurationContainer container;

        private final Node node;

        public SkipReference( final ConfigurationContainer container, final Node node )
        {
            this.container = container;
            this.node = node;
        }

        public ConfigurationContainer getContainer()
        {
            return container;
        }

        public Node getNode()
        {
            return node;
        }

    }

}
