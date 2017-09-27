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
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.ext.manip.impl.PluginManipulator;

import java.util.Properties;

/**
 * Captures configuration relating to plugin alignment from the POMs. Used by {@link PluginManipulator}.
 */
public class CommonState
    implements State
{
    /**
     * Whether to override dependencies that are not directly specified in the project
     */
    private static final String TRANSITIVE_OVERRIDE_PROPERTY = "overrideTransitive";

    /**
     * When true, clashes with cached properties will throw an exception in PropertyResolver. Setting this to false will prevent
     * that. Default value is true.
     * TODO: Might need to be used by pluginManipulator as well
     */
    private static final String PROPERTY_CLASH_FAILS = "propertyClashFails";

    /**
     * Whether to override transitive as well. This is common between {@link DependencyState} and
     * {@liink DependencyState}
     */
    private final boolean overrideTransitive;

    private final boolean propertyClashFails;

    public CommonState( final Properties userProps )
    {
        overrideTransitive = Boolean.valueOf( userProps.getProperty( TRANSITIVE_OVERRIDE_PROPERTY, "true" ) );
        propertyClashFails = Boolean.valueOf( userProps.getProperty( PROPERTY_CLASH_FAILS, "true" ) );
    }

    @Override
    /**
     * Normally isEnabled is tied to the State of the Manipulator. However because these are common properties that do
     * not activate PME but dependent upon other Manipulator (specifically Dependency and Plugin) properties to activate
     * this should always return false so not to confuse PME whether its active or not.
     */
    public boolean isEnabled()
    {
        return false;
    }

    public boolean getPropertyClashFails()
    {
        return propertyClashFails;
    }

    /**
     * @return whether to override unmanaged transitive plugins in the build. Has the effect of adding (or not) new entries
     * to dependency management when no matching dependency is found in the pom. Defaults to true.
     */
    public boolean getOverrideTransitive()
    {
        return overrideTransitive;
    }
}
