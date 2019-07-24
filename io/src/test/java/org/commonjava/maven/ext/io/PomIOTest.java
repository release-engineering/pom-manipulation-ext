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
import org.apache.maven.model.Model;
import org.commonjava.maven.ext.common.json.GAV;
import org.commonjava.maven.ext.common.model.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.net.URL;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PomIOTest
{
    private static final String filename = "pom.xml";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private PomIO pomIO;

    @Before
    public void setup()
    {
        pomIO = new PomIO();
    }

    @Test
    public void testRewritePOMs()
                    throws Exception
    {
        URL resource = PomIOTest.class.getResource( filename );
        assertNotNull( resource );
        File pom = new File( resource.getFile() );
        assertTrue( pom.exists() );

        File targetFile = folder.newFile( "target.xml" );
        FileUtils.copyFile( pom, targetFile );

        Model model = new Model();
        model.setGroupId( "org.commonjava.maven.ext.versioning.test" );
        model.setArtifactId( "dospom" );
        model.setVersion( "1.0" );
        model.setPackaging( "pom" );
        model.setModelVersion( "4.0.0" );

        Project p = new Project( targetFile, model );
        HashSet<Project> changed = new HashSet<>();
        changed.add( p );

        pomIO.rewritePOMs( changed );

        assertTrue( FileUtils.contentEqualsIgnoreEOL( pom, targetFile, "UTF-8" ) );
        assertTrue( FileUtils.contentEquals( targetFile, pom ) );
    }

    @Test
    public void testGAVReturnPOMs()
                    throws Exception
    {
        URL resource = PomIOTest.class.getResource( filename );
        assertNotNull( resource );
        File pom = new File( resource.getFile() );
        assertTrue( pom.exists() );

        File targetFile = folder.newFile( "target.xml" );
        FileUtils.copyFile( pom, targetFile );

        Model model = new Model();
        model.setGroupId( "org.commonjava.maven.ext.versioning.test" );
        model.setArtifactId( "dospom" );
        model.setVersion( "1.0" );
        model.setPackaging( "pom" );
        model.setModelVersion( "4.0.0" );

        Project p = new Project( targetFile, model );
        p.setExecutionRoot();
        HashSet<Project> changed = new HashSet<>();
        changed.add( p );

        GAV gav = pomIO.rewritePOMs( changed );

        assertTrue( gav.getVersion().equals( "1.0" ));
        assertTrue( gav.getGroupId().equals( "org.commonjava.maven.ext.versioning.test" ));
        assertTrue( gav.getArtifactId().equals( "dospom" ));
    }


    @Test
    public void testWriteModel()
                    throws Exception
    {
        // Thanks to http://www.buildmystring.com
        String sb = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                        + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n"
                        + "  <modelVersion>4.0.0</modelVersion>\n"
                        + "  <groupId>org.commonjava.maven.ext.versioning.test</groupId>\n"
                        + "  <artifactId>dospom</artifactId>\n" + "  <version>1.0</version>\n"
                        + "  <packaging>pom</packaging>\n" + "</project>\n";

        URL resource = PomIOTest.class.getResource( filename );
        assertNotNull( resource );
        File pom = new File( resource.getFile() );
        assertTrue( pom.exists() );

        File targetFile = folder.newFile( "target.xml" );

        Model model = new Model();
        model.setGroupId( "org.commonjava.maven.ext.versioning.test" );
        model.setArtifactId( "dospom" );
        model.setVersion( "1.0" );
        model.setPackaging( "pom" );
        model.setModelVersion( "4.0.0" );

        pomIO.writeModel( model, targetFile );
        assertTrue( targetFile.exists() );
        assertEquals( sb, FileUtils.readFileToString( targetFile ) );
    }
}
