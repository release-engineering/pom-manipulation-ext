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
     * When true, it will ignore any suffix ( e.g. rebuild-2 ) on the source version during comparisons. Further, it will
     * only allow alignment to a higher incrementing suffix (e.g. rebuild-3 ).
     */
    public static final String STRICT_ALIGNMENT_IGNORE_SUFFIX = "strictAlignmentIgnoreSuffix";

    /**
     * Whether to override dependencies that are not directly specified in the project
     */
    private static final String TRANSITIVE_OVERRIDE_PROPERTY = "overrideTransitive";

    /**
     * When true, clashes with cached properties will throw an exception in PropertyResolver. Setting this to false will prevent
     * that. Default value is true.
     * TODO: ### Might need to be used by pluginManipulator as well
     */
    private static final String PROPERTY_CLASH_FAILS = "propertyClashFails";

    /**
     * Enables strict checking of non-exclusion dependency versions before aligning to the given BOM dependencies.
     * For example, <code>1.1</code> will match <code>1.1-rebuild-1</code> in strict mode, but <code>1.2</code> will not.
     */
    static final String STRICT_ALIGNMENT = "strictAlignment";

    /**
     * When false, strict version-alignment violations will be reported in the warning log-level, but WILL NOT FAIL THE BUILD. When true, the build
     * will fail if such a violation is detected. Default value is false.
     */
    static final String STRICT_VIOLATION_FAILS = "strictViolationFails";

    /**
     * Whether to override transitive as well. This is common between {@link DependencyState} and
     * {@link DependencyState}
     */
    private final boolean overrideTransitive;

    private final boolean propertyClashFails;

    private final boolean strict;

    private final boolean failOnStrictViolation;

    private final boolean ignoreSuffix;

    public CommonState( final Properties userProps )
    {
        overrideTransitive = Boolean.valueOf( userProps.getProperty( TRANSITIVE_OVERRIDE_PROPERTY, "true" ) );
        propertyClashFails = Boolean.valueOf( userProps.getProperty( PROPERTY_CLASH_FAILS, "true" ) );
        strict = Boolean.valueOf( userProps.getProperty( STRICT_ALIGNMENT, "false" ) );
        ignoreSuffix = Boolean.valueOf( userProps.getProperty( STRICT_ALIGNMENT_IGNORE_SUFFIX, "false" ) );
        failOnStrictViolation = Boolean.valueOf( userProps.getProperty( STRICT_VIOLATION_FAILS, "false" ) );
    }

    /**
     * Normally isEnabled is tied to the State of the Manipulator. However because these are common properties that do
     * not activate PME but dependent upon other Manipulator (specifically Dependency and Plugin) properties to activate
     * this should always return false so not to confuse PME whether its active or not.
     */
    @Override
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

    public boolean getStrict()
    {
        return strict;
    }

    public boolean getStrictIgnoreSuffix()
    {
        return ignoreSuffix;
    }

    public boolean getFailOnStrictViolation()
    {
        return failOnStrictViolation;
    }
}
