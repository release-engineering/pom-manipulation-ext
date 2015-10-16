/**
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
package org.commonjava.maven.ext.manip.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleVersionlessArtifactRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.ext.manip.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience utilities for converting {@link Model} and {@link MavenProject} instances to GA / GAV strings.
 *
 * @author jdcasey
 */
public final class IdUtils
{
    private static final Logger logger = LoggerFactory.getLogger( IdUtils.class );

    private IdUtils()
    {
    }

    /**
     * Splits the value on ',', then wraps each value in {@link SimpleProjectVersionRef#parse(String)} and prints a warning / skips in the event of a
     * parsing error. Returns null if the input value is null.
     * @param value a comma separated list of GAV to parse
     * @return a collection of parsed ProjectVersionRef.
     */
    public static List<ProjectVersionRef> parseGAVs( final String value )
    {
        if ( value == null || value.length () == 0)
        {
            return null;
        }
        else
        {
            final String[] gavs = value.split( "," );
            final List<ProjectVersionRef> refs = new ArrayList<ProjectVersionRef>();
            for ( final String gav : gavs )
            {
                try
                {
                    final ProjectVersionRef ref = SimpleProjectVersionRef.parse( gav );
                    refs.add( ref );
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "Skipping invalid remote management GAV: " + gav );
                    throw e;
                }
            }

            return refs;
        }
    }

    /**
     * Splits the value on ',', then wraps each value in {@link SimpleProjectRef#parse(String)} and prints a warning / skips in the event of a
     * parsing error. Returns null if the input value is null.
     * @param value a comma separated list of GA to parse
     * @return a collection of parsed ProjectRef.
     */
    public static List<ProjectRef> parseGAs( final String value )
    {
        if ( value == null || value.length () == 0)
        {
            return null;
        }
        else
        {
            final String[] gavs = value.split( "," );
            final List<ProjectRef> refs = new ArrayList<ProjectRef>();
            for ( final String gav : gavs )
            {
                try
                {
                    final ProjectRef ref = SimpleProjectRef.parse( gav );
                    refs.add( ref );
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "Skipping invalid remote management GAV: " + gav );
                    throw e;
                }
            }

            return refs;
        }
    }

    public static String gav( final MavenProject project )
    {
        return String.format( "%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    public static String gav( final Project project )
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

    public static String ga( final Project project )
    {
        return ga( project.getGroupId(), project.getArtifactId() );
    }

    public static String ga( final Parent project )
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
