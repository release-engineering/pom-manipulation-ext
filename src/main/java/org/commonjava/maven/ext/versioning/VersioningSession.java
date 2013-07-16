package org.commonjava.maven.ext.versioning;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;

public class VersioningSession
{

    private static VersioningSession INSTANCE = new VersioningSession();

    private MavenExecutionRequest request;

    private Map<String, String> versioningChanges;

    private final Set<String> changedGAVs = new HashSet<String>();

    private boolean enabled;

    // FIXME: Find SOME better way than a classical singleton to house this state!!!
    public static VersioningSession getInstance()
    {
        return INSTANCE;
    }

    public void setRequest( final MavenExecutionRequest request )
    {
        this.request = request;
        final Properties userProps = request.getUserProperties();
        this.enabled =
            userProps.getProperty( VersionCalculator.INCREMENT_SERIAL_SYSPROP ) != null
                || userProps.getProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP ) != null;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public MavenExecutionRequest getRequest()
    {
        return request;
    }

    public void setVersioningChanges( final Map<String, String> versioningChanges )
    {
        this.versioningChanges = versioningChanges;
    }

    public Map<String, String> getVersioningChanges()
    {
        return versioningChanges;
    }

    public Set<String> getChangedGAVs()
    {
        return changedGAVs;
    }

}
