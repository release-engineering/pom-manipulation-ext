package org.commonjava.maven.ext.manip.state;

import java.util.Map;
import java.util.Properties;

import org.commonjava.maven.ext.manip.impl.ProjectVersioningManipulator;

/**
 * Captures configuration and changes relating to the projects' versions. Used by {@link ProjectVersioningManipulator}.
 * 
 * @author jdcasey
 */
public class VersioningState
{

    public static final String VERSION_SUFFIX_SYSPROP = "version.suffix";

    public static final String INCREMENT_SERIAL_SUFFIX_SYSPROP = "version.incremental.suffix";

    public static final String VERSION_SUFFIX_SNAPSHOT_SYSPROP = "version.suffix.snapshot";

    private Map<String, String> versioningChanges;

    private final String suffix;

    private final String incrementSerialSuffix;

    private final boolean preserveSnapshot;

    public VersioningState( final Properties userProps )
    {
        suffix = userProps.getProperty( VERSION_SUFFIX_SYSPROP );
        incrementSerialSuffix = userProps.getProperty( INCREMENT_SERIAL_SUFFIX_SYSPROP );
        preserveSnapshot = Boolean.parseBoolean( userProps.getProperty( VERSION_SUFFIX_SNAPSHOT_SYSPROP ) );
    }

    public void setVersioningChanges( final Map<String, String> versioningChanges )
    {
        this.versioningChanges = versioningChanges;
    }

    public Map<String, String> getVersioningChanges()
    {
        return versioningChanges;
    }

    public String getIncrementalSerialSuffix()
    {
        return incrementSerialSuffix;
    }

    public String getSuffix()
    {
        return suffix;
    }

    public boolean preserveSnapshot()
    {
        return preserveSnapshot;
    }

    /**
     * Enabled ONLY if either version.incremental.suffix or version.suffix is provided in the user properties / CLI -D options.
     * 
     * @see #VERSION_SUFFIX_SYSPROP
     * @see #INCREMENT_SERIAL_SUFFIX_SYSPROP
     */
    public boolean isEnabled()
    {
        return incrementSerialSuffix != null || suffix != null;
    }

}
