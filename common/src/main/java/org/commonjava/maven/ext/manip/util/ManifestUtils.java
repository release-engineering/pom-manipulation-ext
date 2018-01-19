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

package org.commonjava.maven.ext.manip.util;

import org.commonjava.maven.ext.manip.ManipulationException;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

public class ManifestUtils
{
    /**
     * Retrieves the SHA this was built with.
     *
     * @return the GIT sha of this codebase.
     * @throws ManipulationException if an error occurs.
     */
    public static String getManifestInformation()
        throws ManipulationException
    {
        String result = "";
        try
        {
            final Enumeration<URL> resources = ManifestUtils.class.getClassLoader()
                                                          .getResources( "META-INF/MANIFEST.MF" );

            while ( resources.hasMoreElements() )
            {
                final URL jarUrl = resources.nextElement();

                if ( jarUrl.getFile()
                           .contains( "pom-manipulation-" ) )
                {
                    final Manifest manifest = new Manifest( jarUrl.openStream() );

                    result = manifest.getMainAttributes()
                                     .getValue( "Implementation-Version" );
                    result += " ( SHA: " + manifest.getMainAttributes()
                                                   .getValue( "Scm-Revision" ) + " ) ";
                    break;
                }
            }
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Error retrieving information from manifest", e );
        }

        return result;
    }
}
