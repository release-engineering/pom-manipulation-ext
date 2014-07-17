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
package org.commonjava.maven.ext.manip.state;

import java.util.Properties;

import org.commonjava.maven.ext.manip.impl.PluginManipulator;

/**
 * Captures configuration relating to plugin alignment from the POMs. Used by {@link PluginManipulator}.
 *
 */
public class PluginState
    implements State
{
    /**
     * The name of the property which contains the GAV of the remote pom from which to retrieve plugin management
     * information. <br />
     * <code>-DpluginManagement:org.foo:bar-plugin-mgmt:1.0</code>
     */
    private static final String PLUGIN_MANAGEMENT_POM_PROPERTY = "pluginManagement";

    private final String pluginMgmt;

    public PluginState( final Properties userProps )
    {
        pluginMgmt = userProps.getProperty( PLUGIN_MANAGEMENT_POM_PROPERTY );
    }

    /**
     * Enabled ONLY if repo-reporting-removal is provided in the user properties / CLI -D options.
     *
     * @see #ENFORCE_SYSPROP
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return ( pluginMgmt != null && pluginMgmt.length() > 0 );
    }

    public String getRemotePluginMgmt()
    {
        return pluginMgmt;
    }
}
