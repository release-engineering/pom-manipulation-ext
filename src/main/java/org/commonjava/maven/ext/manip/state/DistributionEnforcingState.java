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
    
    public enum EnforcingMode{
        /**
         * Enforce that the skip flag be <b>ENABLED</b>, disabling install and deployment. 
         * This is normally used in conjunction with {@link DistributionEnforcingState#PROJECT_EXCLUSION_PREFIX} 
         * to customize enforcement for a single project within a larger build.
         */
        on
        {
            @Override
            public Boolean defaultModificationValue()
            {
                return true;
            }
        },
        /**
         * Enforce that the skip flag be <b>DISABLED</b>, enabling install and deployment.
         */
        off
        {
            @Override
            public Boolean defaultModificationValue()
            {
                return false;
            }
        },
        /**
         * Detect the appropriate skip-flag mode from the first available install-plugin configuration in the main pom. Only configurations in the
         * plugin-wide config section and those in the <code>default-install</code> plugin execution will be considered.
         */
        detect
        {
            @Override
            public Boolean defaultModificationValue()
            {
                return null;
            }
        },
        /**
         * Disable skip-flag enforcement.
         * This is normally used in conjunction with {@link DistributionEnforcingState#PROJECT_EXCLUSION_PREFIX} 
         * to disable enforcement for a single project within a larger build.
         */
        none
        {
            @Override
            public Boolean defaultModificationValue()
            {
                return null;
            }
        };

        public static EnforcingMode getMode( final String value )
        {
            if ( value == null || value.trim()
                                       .length() < 1 )
            {
                return off;
            }

            if ( Boolean.parseBoolean( value ) )
            {
                return on;
            }

            EnforcingMode m = valueOf( value );

            if ( m == null && value.equalsIgnoreCase( "false" ) )
            {
                m = off;
            }

            return m == null ? none : m;
        }

        public abstract Boolean defaultModificationValue();
    }

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
