/*
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.commonjava.maven.ext.core.util;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Convenience utilities for converting between {@link ProjectVersionRef}, {@link Model}, {@link MavenProject} and GA / GAV strings.
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
     * Splits the value on ',', then wraps each value in {@link SimpleProjectVersionRef#parse(String)}. Returns null
     * if the input value is null.
     * @param value a comma separated list of GAV to parse
     * @return a collection of parsed ProjectVersionRef.
     */
    public static List<ProjectVersionRef> parseGAVs( final String value )
    {
        if ( isEmpty( value ) )
        {
            return null;
        }
        else
        {
            final String[] gavs = value.split( "," );
            final List<ProjectVersionRef> refs = new ArrayList<>();
            for ( final String gav : gavs )
            {
                if (isNotEmpty( gav ))
                {
                    if ( gav.startsWith( "http://" ) || gav.startsWith( "https://") )
                    {
                        logger.debug( "Found remote file in {}", gav );
                        try
                        {
                            File found = File.createTempFile( UUID.randomUUID().toString(), null );
                            FileUtils.copyURLToFile( new URL( gav ), found );
                            String potentialRefs =
                                            FileUtils.readFileToString( found, Charset.defaultCharset() ).trim().replace( "\n", "," );
                            List<ProjectVersionRef> readRefs = parseGAVs( potentialRefs );
                            if ( readRefs != null )
                            {
                                refs.addAll( readRefs );
                            }
                        }
                        catch ( InvalidRefException | IOException e )
                        {
                            throw new ManipulationUncheckedException( e );
                        }
                    }
                    else
                    {
                        refs.add( SimpleProjectVersionRef.parse( gav ) );
                    }
                }
            }
            return refs;
        }
    }

    /**
     * Splits the value on ',', then wraps each value in {@link SimpleProjectRef#parse(String)}. Returns null if the
     * input value is null.
     * @param value a comma separated list of GA to parse
     * @return a collection of parsed ProjectRef.
     */
    public static List<ProjectRef> parseGAs( final String value )
    {
        if ( isEmpty( value ) )
        {
            return null;
        }
        else
        {
            final String[] gavs = value.split( "," );
            final List<ProjectRef> refs = new ArrayList<>();
            for ( final String gav : gavs )
            {
                if (isNotEmpty( gav ))
                {
                    if ( gav.startsWith( "http://" ) || gav.startsWith( "https://") )
                    {
                        logger.debug( "Found remote file in {}", gav );
                        try
                        {
                            File found = File.createTempFile( UUID.randomUUID().toString(), null );
                            FileUtils.copyURLToFile( new URL( gav ), found );
                            String potentialRefs =
                                            FileUtils.readFileToString( found, Charset.defaultCharset() ).trim().replace( "\n", "," );
                            List<ProjectRef> readRefs = parseGAs( potentialRefs );
                            if ( readRefs != null )
                            {
                                refs.addAll( readRefs );
                            }
                        }
                        catch ( InvalidRefException | IOException e )
                        {
                            throw new ManipulationUncheckedException( e );
                        }
                    }
                    else
                    {
                        refs.add( SimpleProjectRef.parse( gav ) );
                    }
                }
            }

            return refs;
        }
    }

    public static String gav( final Project project )
    {
        return String.format( "%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion() );
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
