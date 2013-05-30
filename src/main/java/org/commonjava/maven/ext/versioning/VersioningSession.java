package org.commonjava.maven.ext.versioning;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;

public class VersioningSession
{

    private static VersioningSession INSTANCE = new VersioningSession();

    private MavenExecutionRequest request;

    private Map<String, String> versioningChanges;

    private final Set<String> changedGAVs = new HashSet<String>();

    public static VersioningSession getInstance()
    {
        return INSTANCE;
    }

    public void setRequest( final MavenExecutionRequest request )
    {
        this.request = request;
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
