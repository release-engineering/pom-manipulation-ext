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

import org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator;

/**
 * Captures configuration relating to enforcing distribution (install/deploy) configurations within the POM(s). Used by {@link DistributionEnforcingManipulator}.
 */
public class DistributionEnforcingState
    implements State
{
    /**
     * Property used to set the enforcement mode.
     */
    public static final String ENFORCE_SYSPROP = "enforce-skip";

    /**
     * Propery prefix used to exclude certain projects in the current build (specified as g:a) from install/deploy plugin skip-flag enforcement.
     */
    public static final String PROJECT_EXCLUSION_PREFIX = "enforceSkip.";
    
    private final EnforcingMode mode;

    public DistributionEnforcingState( final Properties userProps )
    {
        final String value = userProps.getProperty( ENFORCE_SYSPROP );
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
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     * @see EnforcingMode
     */
    @Override
    public boolean isEnabled()
    {
        return mode != EnforcingMode.none;
    }

}
