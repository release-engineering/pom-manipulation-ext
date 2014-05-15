package org.commonjava.maven.ext.manip.state;

import java.util.Properties;

import org.commonjava.maven.ext.manip.impl.RepoAndReportingRemovalManipulator;

/**
 * Captures configuration relating to removing reporting/repositories from the POMs. Used by {@link RepoAndReportingRemovalManipulator}.
 *
 */
public class BOMState implements State
{
    /**
     * The character used to separate groupId:arifactId:version
     */
    public static final String GAV_SEPERATOR = ":";

    /**
     * Suffix to enable this modder. The name of the property which contains the GAV of the remote pom from
     * which to retrieve property mapping information. <br />
     * ex: -DpropertyManagement:org.foo:bar-property-mgmt:1.0
     */
    public static final String PROPERTY_MANAGEMENT_POM_PROPERTY = "propertyManagement";

    /**
     * The name of the property which contains the GAV of the remote pom from which to retrieve plugin management
     * information. <br />
     * ex: -DpluginManagement:org.foo:bar-plugin-mgmt:1.0
     */
    private static final String PLUGIN_MANAGEMENT_POM_PROPERTY = "pluginManagement";

    private final String pluginMgmt;

    private final String propertyMgmt;

    public BOMState( final Properties userProps )
    {
        pluginMgmt = userProps.getProperty( PLUGIN_MANAGEMENT_POM_PROPERTY );
        propertyMgmt = userProps.getProperty( PROPERTY_MANAGEMENT_POM_PROPERTY );
   }

    /**
     * Enabled ONLY if repo-reporting-removal is provided in the user properties / CLI -D options.
     *
     * @see #RR_SUFFIX_SYSPROP
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return (pluginMgmt != null && pluginMgmt.length() > 0) || (propertyMgmt != null && propertyMgmt.length() > 0);
    }

    public String getRemotePluginMgmt ()
    {
        return pluginMgmt;
    }

    public String getRemotePropertyMgmt ()
    {
        return propertyMgmt;
    }
}
