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
import org.commonjava.maven.ext.io.rest.handler.StaticResourceHandler;
import org.commonjava.maven.ext.io.rest.rule.MockServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class FileIOTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private StaticResourceHandler staticFile = new StaticResourceHandler( "pom.xml" );

    @Rule
    public MockServer mockServer = new MockServer( staticFile );

    private FileIO fileIO;

    @Before
    public void before()
        throws Exception
    {
        File res = folder.newFolder();
        GalleyInfrastructure galleyInfra = new GalleyInfrastructure
                        ( null, null).init( null, null, res );
        fileIO = new FileIO( galleyInfra );
   }

    @Test
    public void testReadURL() throws Exception
    {
        String urlPom = FileUtils.readFileToString( fileIO.resolveURL( new URL ( mockServer.getUrl()) ), Charset.defaultCharset() );
        String filePom = FileUtils.readFileToString( new File ( new File (FileIOTest.class.getResource( "/" ).getPath())
                                          .getParentFile().getParentFile(), "pom.xml"), "UTF-8" );

        assertEquals( urlPom, filePom );
    }
}
