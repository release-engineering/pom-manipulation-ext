/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.commonjava.maven.ext.manip.io;

import org.apache.maven.model.Model;
import org.apache.commons.io.FileUtils;
import org.commonjava.maven.ext.manip.model.Project;
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
        HashSet<Project> changed = new HashSet<Project>();
        changed.add( p );

        pomIO.rewritePOMs( changed );

        assertTrue( FileUtils.contentEqualsIgnoreEOL( pom, targetFile, "UTF-8" ) );
        assertTrue( FileUtils.contentEquals( targetFile, pom ) );
    }
}