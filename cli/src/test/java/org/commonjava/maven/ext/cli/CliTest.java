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
package org.commonjava.maven.ext.cli;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.maven.execution.MavenSession;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CliTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder( );

    @Rule
    public final SystemOutRule systemRule = new SystemOutRule().enableLog().muteForSuccessfulTests();


    private File writeSettings (File f) throws IOException
    {
        FileUtils.writeStringToFile( f, "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\""
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + "xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">"
                        + "</settings>", Charset.defaultCharset() );
        return f;
    }

    @Test
    public void checkTargetMatches() throws Exception
    {
        Cli c = new Cli();
        File pom1 = temp.newFile( );
        File settings = writeSettings( temp.newFile( ));

        TestUtils.executeMethod( c, "createSession", new Object[] { pom1, settings } );

        assertEquals( "Session file should match", pom1,
                      ( (ManipulationSession) FieldUtils.readField( c, "session", true ) ).getPom() );
    }

    @Test
    public void checkTargetMatchesWithRun() throws Exception
    {
        Cli c = new Cli();
        File pom1 = temp.newFile( );

        TestUtils.executeMethod( c, "run", new Object[] { new String[] { "-f", pom1.toString() } } );

        assertEquals( "Session file should match", pom1,
                      ( (ManipulationSession) FieldUtils.readField( c, "session", true ) ).getPom() );
    }

    @Test
    public void checkTargetDefaultMatches() throws Exception
    {
        Cli c = new Cli();

        TestUtils.executeMethod( c, "run", new Object[] { new String[] {} } );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        File defaultTarget = (File) FieldUtils.readField( c, "target", true );

        assertEquals( "Session file should match", defaultTarget, session.getPom() );
    }

    @Test
    public void checkLocalRepositoryWithDefaults() throws Exception
    {
        Cli c = new Cli();
        File settings = writeSettings( temp.newFile());

        TestUtils.executeMethod( c, "run", new Object[] { new String[] { "-s", settings.toString()} } );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertEquals( ms.getRequest().getLocalRepository().getBasedir(),
                      ms.getRequest().getLocalRepositoryPath().toString() );
        assertEquals( "File " + new File( ms.getRequest().getLocalRepository().getBasedir() ).getParentFile().toString()
                                      + " was not equal to " + System.getProperty( "user.home" ) + File.separatorChar
                                      + ".m2",
                      new File( ms.getRequest().getLocalRepository().getBasedir() ).getParentFile().toString(),
                      System.getProperty( "user.home" ) + File.separatorChar + ".m2" );

    }

    @Test
    public void checkLocalRepositoryWithDefaultsAndModifiedUserSettings() throws Exception
    {
        boolean restore = false;
        Path source = Paths.get ( System.getProperty( "user.home" ) + File.separatorChar + ".m2" + File.separatorChar + "settings.xml");
        Path backup = Paths.get( source.toString() + '.' + UUID.randomUUID().toString() );
        Path tmpSettings = Paths.get ( getClass().getResource("/settings-test.xml").toURI() );

        try
        {
            if ( source.toFile().exists() )
            {
                System.out.println ("Backing up settings.xml to " + backup);
                restore = true;
                Files.move( source, backup, StandardCopyOption.ATOMIC_MOVE);
            }
            Files.copy( tmpSettings, source );

            Cli c = new Cli();
            TestUtils.executeMethod( c, "run", new Object[] { new String[] {} } );

            ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
            MavenSession ms = (MavenSession) FieldUtils.readField( session, "mavenSession", true );

            assertEquals( ms.getRequest().getLocalRepository().getBasedir(),
                          ms.getRequest().getLocalRepositoryPath().toString() );
            assertEquals( ms.getLocalRepository().getBasedir(),
                          System.getProperty( "user.home" ) + File.separatorChar + ".m2-mead-test" );

        }
        finally
        {
            if ( restore )
            {
                Files.move( backup, source, StandardCopyOption.ATOMIC_MOVE);
            }
            else
            {
                Files.delete( source );
            }
        }
    }

    @Test
    public void checkProfileActivation() throws Exception
    {
        File folder = temp.newFolder();
        File target = temp.newFile( );
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File ( TestUtils.resolveFileResource( "", "" )
                                                     .getParentFile()
                                                     .getParentFile()
                                                     .getParentFile(), "integration-test/pom.xml" );
        FileUtils.copyFile( projectroot, target );

        Cli c = new Cli();
        TestUtils.executeMethod( c, "run", new Object[] {
                        new String[] { "-d", "--settings=" + getClass().getResource( "/settings-test.xml" ).getFile(),
                                        "-Dmaven.repo.local=" + folder.toString(), "-Prun-its", "--file",
                                        target.getAbsolutePath() } });

        assertTrue (systemRule.getLog().contains( "Explicitly activating [run-its]" ));
        assertTrue (systemRule.getLog().contains( "Will not scan all profiles and returning active profiles of [run-its]" ));

    }


    @Test
    public void checkLocalRepositoryWithSettings() throws Exception
    {
        Cli c = new Cli();
        TestUtils.executeMethod( c, "run", new Object[] { new String[] { "-settings=" + getClass().getResource( "/settings-test.xml").getFile() }} );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertEquals( ms.getRequest().getLocalRepository().getBasedir(),
                      ms.getRequest().getLocalRepositoryPath().toString() );
    }

    @Test
    public void checkLocalRepositoryWithExplicitMavenRepo() throws Exception
    {
        File folder = temp.newFolder();
        Cli c = new Cli();
        TestUtils.executeMethod( c, "run", new Object[] { new String[] { "-Dmaven.repo.local=" + folder.toString() }} );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertEquals( ms.getRequest().getLocalRepository().getBasedir(),
                      ms.getRequest().getLocalRepositoryPath().toString() );
    }

    @Test
    public void checkLocalRepositoryWithExplicitMavenRepoAndSettings() throws Exception
    {
        File folder = temp.newFolder();
        Cli c = new Cli();
        TestUtils.executeMethod( c, "run", new Object[] { new String[]
                        { "--settings=" + getClass().getResource("/settings-test.xml").getFile(),
                                        "-Dmaven.repo.local=" + folder.toString() }} );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertEquals( ms.getLocalRepository().getBasedir(), folder.toString() );
        assertEquals( ms.getRequest().getLocalRepository().getBasedir(),
                      ms.getRequest().getLocalRepositoryPath().toString() );
    }
}
