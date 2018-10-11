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
import org.commonjava.maven.ext.io.resolver.GalleyInfrastructure;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * Class to resolve Files from alternate locations
 */
@Named
@Singleton
public class FileIO
{
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
}
