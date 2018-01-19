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
package org.commonjava.maven.ext.core.state;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.ext.core.util.IdUtils;
import org.commonjava.maven.ext.core.util.PropertiesUtils;
import org.commonjava.maven.ext.core.util.PropertyFlag;

import java.util.List;
import java.util.Properties;

/**
 * Captures configuration relating to plugin removal from the POMs.
 */
public class PluginRemovalState
    implements State
{
    /**
     * The name of the property which contains a comma separated list of plugins to remove.
     * <pre>
     * <code>-DpluginRemoval=org.foo:bar-plugin,....</code>
     * </pre>
     */
    private static final PropertyFlag PLUGIN_REMOVAL_PROPERTY = new PropertyFlag( "plugin-removal", "pluginRemoval");

    private final List<ProjectRef> pluginRemoval;

    public PluginRemovalState( final Properties userProps )
    {
        pluginRemoval = IdUtils.parseGAs( PropertiesUtils.handleDeprecatedProperty ( userProps, PLUGIN_REMOVAL_PROPERTY ) );
    }

    /**
     * Enabled ONLY if plugin-removal is provided in the user properties / CLI -D options.
     *
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return pluginRemoval != null && !pluginRemoval.isEmpty();
    }

    public List<ProjectRef> getPluginRemoval()
    {
        return pluginRemoval;
    }
}
