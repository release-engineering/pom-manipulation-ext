package org.commonjava.maven.ext.manip;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

/**
 * Convenience utilities for converting {@link Model} and {@link MavenProject} instances to GA / GAV strings.
 *
 * @author jdcasey
 */
public final class IdUtils
{
    /**
     * Regex pattern for parsing a Maven GAV
     */
    public static final Pattern gavPattern = Pattern.compile( "\\s*([\\w\\-_.]+):([\\w\\-_.]+):(\\d[\\w\\-_.]+)\\s*" );


    private IdUtils()
    {
    }

    public static boolean validGav(String gav)
    {
        Matcher matcher = gavPattern.matcher( gav );
        return matcher.matches();
    }

    public static String gav( final MavenProject project )
    {
        return String.format( "%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    public static String gav( final Model model )
    {
        String g = model.getGroupId();
        String v = model.getVersion();

        final Parent p = model.getParent();
        if ( p != null )
        {
            if ( g == null )
            {
                g = p.getGroupId();
            }

            if ( v == null )
            {
                v = p.getVersion();
            }
        }

        return String.format( "%s:%s:%s", g, model.getArtifactId(), v );
    }

    public static String ga( final Model model )
    {
        String g = model.getGroupId();

        final Parent p = model.getParent();
        if ( p != null )
        {
            if ( g == null )
            {
                g = p.getGroupId();
            }
        }

        return ga( g, model.getArtifactId() );
    }

    public static String ga( final MavenProject project )
    {
        return ga( project.getGroupId(), project.getArtifactId() );
    }

    public static String ga( final String g, final String a )
    {
        return String.format( "%s:%s", g, a );
    }

    public static String gav( final String g, final String a, final String v )
    {
        return String.format( "%s:%s:%s", g, a, v );
    }
}
