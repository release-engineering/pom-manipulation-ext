package org.commonjava.maven.ext.manip.state;

public enum EnforcingMode
{
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
            return none;
        }

        if ( Boolean.parseBoolean( value ) )
        {
            return on;
        }

        if ( value.equalsIgnoreCase( "false" ) )
        {
            return off;
        }

        EnforcingMode m = none;

        final EnforcingMode[] values = values();
        for ( final EnforcingMode mode : values )
        {
            if ( value.equalsIgnoreCase( mode.name() ) )
            {
                m = mode;
                break;
            }
        }

        return m;
    }

    public abstract Boolean defaultModificationValue();
}