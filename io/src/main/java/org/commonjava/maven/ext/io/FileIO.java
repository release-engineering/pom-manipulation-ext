/*
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
package org.commonjava.maven.ext.io;

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.io.resolver.GalleyInfrastructure;
import org.jdom2.output.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Class to resolve Files from alternate locations
 */
@Named
@Singleton
public class FileIO
{
    private static final Logger logger = LoggerFactory.getLogger( FileIO.class );

    private GalleyInfrastructure infra;

    @Inject
    public FileIO(@Named("galley") GalleyInfrastructure infra)
    {
        this.infra = infra;
    }

    /**
     * Read the raw file from a given URL. Useful if we need to read
     * a remote file.
     *
     * @param ref the ArtifactRef to read.
     * @return the file for the URL
     * @throws IOException if an error occurs.
     */
    public File resolveURL( final URL ref ) throws IOException
    {
        File cache = infra.getCacheDir();
        File result = new File( cache, UUID.randomUUID().toString() );

        FileUtils.copyURLToFile( ref, result );

        return result;
    }

    static LineSeparator determineEOL( File file )
        throws ManipulationException
    {
        return determineEOL( file, StandardCharsets.UTF_8 );
    }

    static LineSeparator determineEOL( File file, Charset charset )
            throws ManipulationException
    {
        try ( BufferedReader bufferIn = new BufferedReader( new InputStreamReader( new FileInputStream( file ),
                charset ) ) )
        {
            int prev = -1;
            int ch;
            while ( ( ch = bufferIn.read() ) != -1 )
            {
                if ( ch == '\n' )
                {
                    if ( prev == '\r' )
                    {
                        return LineSeparator.CRNL;
                    }
                    else
                    {
                        return LineSeparator.NL;
                    }
                }
                else if ( prev == '\r' )
                {
                    return LineSeparator.CR;
                }
                prev = ch;
            }
            throw new ManipulationException( "Could not determine end-of-line marker mode" );
        }
        catch ( IOException ioe )
        {
            throw new ManipulationException( "Could not determine end-of-line marker mode", ioe );
        }
    }
}
