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
package org.commonjava.maven.ext.manip;

import ch.qos.logback.classic.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.maven.execution.MavenSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class CliTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder( );

    @Before
    public void before()
    {
        final ch.qos.logback.classic.Logger root =
                        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
        root.setLevel( Level.OFF );
    }

    private File writeSettings (File f) throws IOException
    {
        FileUtils.writeStringToFile( f, "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\""
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + "xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">"
                        + "</settings>" );
        return f;
    }

    @Test
    public void checkTargetMatches() throws Exception
    {
        Cli c = new Cli();
        File pom1 = temp.newFile( );
        File settings = writeSettings( temp.newFile( ));

        executeMethod( c, "createSession", new Object[] { pom1, settings } );

        assertTrue( "Session file should match",
                    pom1.equals( ( (ManipulationSession) FieldUtils.readField( c, "session", true ) ).getPom() ) );
    }

    @Test
    public void checkTargetMatchesWithRun() throws Exception
    {
        Cli c = new Cli();
        File pom1 = temp.newFile( );

        executeMethod( c, "run", new Object[] { new String[] { "-f", pom1.toString() } } );

        assertTrue( "Session file should match",
                    pom1.equals( ( (ManipulationSession) FieldUtils.readField( c, "session", true ) ).getPom() ) );
    }

    @Test
    public void checkTargetDefaultMatches() throws Exception
    {
        Cli c = new Cli();

        executeMethod( c, "run", new Object[] { new String[] {} } );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        File defaultTarget = (File) FieldUtils.readField( c, "target", true );

        assertTrue( "Session file should match", defaultTarget.equals( session.getPom() ) );
    }

    @Test
    public void checkLocalRepositoryWithDefaults() throws Exception
    {
        Cli c = new Cli();
        File settings = writeSettings( temp.newFile());

        executeMethod( c, "run", new Object[] { new String[] { "-s", settings.toString()} } );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertTrue( ms.getRequest().getLocalRepository().getBasedir().equals( ms.getRequest().getLocalRepositoryPath().toString() ) );
        assertTrue ( "File " + new File ( ms.getRequest().getLocalRepository().getBasedir() ).getParentFile().toString() +
                                     " was not equal to " + System.getProperty( "user.home" ) + File.separatorChar + ".m2",
                        new File ( ms.getRequest().getLocalRepository().getBasedir() ).getParentFile().toString().
                        equals( System.getProperty( "user.home" ) + File.separatorChar + ".m2" ) );

    }

    @Test
    public void checkLocalRepositoryWithDefaultsAndModifiedUserSettings() throws Exception
    {
        boolean restore = false;
        Path source = Paths.get ( System.getProperty( "user.home" ) + File.separatorChar + ".m2" + File.separatorChar + "settings.xml");
        Path backup = Paths.get( source.toString() + '.' + UUID.randomUUID().toString() );
        Path tmpSettings = Paths.get ( getClass().getResource("/settings-test.xml").getFile() );

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
            executeMethod( c, "run", new Object[] { new String[] {} } );

            ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
            MavenSession ms = (MavenSession) FieldUtils.readField( session, "mavenSession", true );

            assertTrue( ms.getRequest()
                          .getLocalRepository()
                          .getBasedir()
                          .equals( ms.getRequest().getLocalRepositoryPath().toString() ) );
            assertTrue( ms.getLocalRepository().getBasedir().equals( System.getProperty( "user.home" ) + File.separatorChar + ".m2-mead-test" ) );

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
    public void checkLocalRepositoryWithSettings() throws Exception
    {
        Cli c = new Cli();
        executeMethod( c, "run", new Object[] { new String[] { "-settings=" + getClass().getResource("/settings-test.xml").getFile() }} );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertTrue( ms.getRequest().getLocalRepository().getBasedir().equals( ms.getRequest().getLocalRepositoryPath().toString() ) );
    }

    @Test
    public void checkLocalRepositoryWithExplicitMavenRepo() throws Exception
    {
        File folder = temp.newFolder();
        Cli c = new Cli();
        executeMethod( c, "run", new Object[] { new String[] { "-Dmaven.repo.local=" + folder.toString() }} );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertTrue( ms.getRequest().getLocalRepository().getBasedir().equals( ms.getRequest().getLocalRepositoryPath().toString() ) );
    }

    @Test
    public void checkLocalRepositoryWithExplicitMavenRepoAndSettings() throws Exception
    {
        File folder = temp.newFolder();
        Cli c = new Cli();
        executeMethod( c, "run", new Object[] { new String[]
                        { "-settings=" + getClass().getResource("/settings-test.xml").getFile(),
                                        "-Dmaven.repo.local=" + folder.toString() }} );

        ManipulationSession session = (ManipulationSession) FieldUtils.readField( c, "session", true );
        MavenSession ms = (MavenSession)FieldUtils.readField( session, "mavenSession", true );

        assertTrue( ms.getLocalRepository().getBasedir().equals( folder.toString() ) );
        assertTrue( ms.getRequest().getLocalRepository().getBasedir().equals( ms.getRequest().getLocalRepositoryPath().toString() ) );
    }

    /**
     * Executes a method on an object instance.  The name and parameters of
     * the method are specified.  The method will be executed and the value
     * of it returned, even if the method would have private or protected access.
     */
    private Object executeMethod( Object instance, String name, Object[] params ) throws Exception
    {
        Class c = instance.getClass();

        // Fetch the Class types of all method parameters
        Class[] types = new Class[params.length];

        for ( int i = 0; i < params.length; i++ )
            types[i] = params[i].getClass();

        Method m = c.getDeclaredMethod( name, types );
        m.setAccessible( true );

        return m.invoke( instance, params );
    }
}
