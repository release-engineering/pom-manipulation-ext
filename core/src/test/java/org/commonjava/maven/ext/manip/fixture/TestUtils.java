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

package org.commonjava.maven.ext.manip.fixture;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.commonjava.maven.ext.manip.ManipulationException;

import java.io.File;
import java.io.FileReader;
import java.net.URL;

public class TestUtils
{
    public static Model resolveModelResource( final String resourceBase, final String resourceName )
        throws Exception
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( resourceBase + resourceName );

        if ( resource == null )
        {
            throw new ManipulationException( "Unable to locate resource for " + resourceBase + resourceName );
        }
        return new MavenXpp3Reader().read( new FileReader( new File( resource.getPath() ) ) );
    }
}
