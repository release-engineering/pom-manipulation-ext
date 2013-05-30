package org.commonjava.maven.ext.versioning;

import org.apache.maven.project.MavenProject;

public final class IdUtils
{

    private IdUtils()
    {
    }

    public static String gav( final MavenProject project )
    {
        return String.format( "%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    public static String ga( final String g, final String a )
    {
        return String.format( "%s:%s", g, a );
    }
}
