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

import org.commonjava.maven.ext.core.impl.DistributionEnforcingManipulator;
import org.commonjava.maven.ext.core.util.PropertiesUtils;
import org.commonjava.maven.ext.core.util.PropertyFlag;

import java.util.Map;
import java.util.Properties;

import static org.commonjava.maven.ext.core.util.PropertiesUtils.getPropertiesByPrefix;

/**
 * Captures configuration relating to enforcing distribution (install/deploy) configurations within the POM(s). Used by {@link DistributionEnforcingManipulator}.
 */
public class DistributionEnforcingState
    implements State
{
    /**
     * Property used to set the enforcement mode.
     */
    public static final PropertyFlag ENFORCE_SYSPROP = new PropertyFlag( "enforce-skip", "enforceSkip");

    /**
     * Property prefix used to exclude certain projects in the current build (specified as g:a) from install/deploy plugin skip-flag enforcement.
     */
    private static final String PROJECT_EXCLUSION_PREFIX = "enforceSkip.";
    
    private final EnforcingMode mode;

    private final Map<String, String> excludedProjects;

    public DistributionEnforcingState( final Properties userProps )
    {
        final String value = PropertiesUtils.handleDeprecatedProperty (userProps, ENFORCE_SYSPROP );

        this.excludedProjects = getPropertiesByPrefix( userProps, DistributionEnforcingState.PROJECT_EXCLUSION_PREFIX );
        this.mode = EnforcingMode.getMode( value );
    }
    
    public EnforcingMode getEnforcingMode()
    {
        return mode;
    }

    /**
     * Enabled ONLY if enforce-skip is provided in the user properties / CLI -D options, AND that mode is != <code>none</code>.
     *
     * @see #ENFORCE_SYSPROP
     * @see org.commonjava.maven.ext.core.state.State#isEnabled()
     * @see EnforcingMode
     */
    @Override
    public boolean isEnabled()
    {
        return mode != EnforcingMode.none;
    }

    public Map<String, String> getExcludedProjects()
    {
        return excludedProjects;
    }
}
