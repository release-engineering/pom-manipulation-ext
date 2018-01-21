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
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.io.resolver.ExtensionInfrastructure;
import org.commonjava.maven.ext.io.resolver.GalleyInfrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * Class to resolve Files from alternate locations
 */
@Component( role = FileIO.class )
public class FileIO
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement( role = ExtensionInfrastructure.class, hint = "galley" )
    private GalleyInfrastructure infra;

    /**
     * Read the raw file from a given URL. Useful if we need to read
     * a remote file.
     *
     * @param ref the ArtifactRef to read.
     * @return the file for the URL
     * @throws ManipulationException if an error occurs.
     */
    public File resolveURL( final URL ref ) throws ManipulationException, IOException
    {
        File cache = infra.getCacheDir();
        File result = new File( cache, UUID.randomUUID().toString() );

        logger.info( "### [temp debug] cache dir is {} and result is {} ", cache, result );

        FileUtils.copyURLToFile( ref, result );

        return result;
    }
}
