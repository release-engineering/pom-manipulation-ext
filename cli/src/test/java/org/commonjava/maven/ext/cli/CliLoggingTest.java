/*
 * Copyright (C) 2012 Red Hat, Inc.
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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.commonjava.maven.ext.core.fixture.TestUtils.ROOT_DIRECTORY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith( BMUnitRunner.class)
@BMUnitConfig(verbose = true, debug = true, bmunitVerbose = true )
public class CliLoggingTest
{
    @SuppressWarnings( "unused" )
    static final Map<String,String> PODMAN_MAP = new HashMap<String,String>() {{
        put( "container", "podman" );
    }};

    @SuppressWarnings( { "unused", "ConstantConditions" } )
    static final String CGROUP_DOCKER = Thread.currentThread().getContextClassLoader().getResource( "cgroup-docker" ).getPath();

    @SuppressWarnings( { "unused", "ConstantConditions" } )
    static final String CGROUP_KUBE = Thread.currentThread().getContextClassLoader().getResource( "cgroup-kubernetes" ).getPath();

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();


    @Test
    public void testLogfile() throws Exception
    {
        File folder = temp.newFolder();
        File target = new File( folder, "pom.xml" );
        File logfile = new File( folder, "logfile" );

        FileUtils.copyDirectory( ROOT_DIRECTORY.toFile(), folder,
                                 FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, FileFilterUtils.suffixFileFilter( "xml" ) ) );

        try
        {
            Cli c = new Cli();
            c.run( new String[] { "-d", "--settings=" + getClass().getResource( "/settings-test.xml" ).getFile(),
                            "-Dmaven.repo.local=" + folder.toString(), "-l", logfile.getCanonicalPath(),
                            "-DversionSuffix=rebuild-1", "--file", target.getCanonicalPath() } );

            assertTrue( logfile.exists() );
            try (Stream<String> stream = Files.lines( logfile.toPath() ))
            {
                assertTrue( stream.anyMatch( line -> line.contains( "Running manipulator" ) ) );
            }
            assertFalse( systemOutRule.getLog().contains( "Running manipulator" ) );
        }
        finally
        {
            // Reload the default configuration otherwise strange errors happen to later tests.
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();
            ContextInitializer ci = new ContextInitializer(loggerContext);
            ci.autoConfig();
        }
    }

    @Test
    @BMRule( name = "fake-env",
             targetClass = "java.lang.System",
             targetMethod = "getenv()",
             targetLocation = "AT ENTRY",
             action = "return org.commonjava.maven.ext.cli.CliLoggingTest.PODMAN_MAP"
    )
    public void testLogfilePodman() throws Exception
    {
        File folder = temp.newFolder();
        File target = new File( folder, "pom.xml" );
        File logfile = new File( folder, "logfile" );

        FileUtils.copyDirectory( ROOT_DIRECTORY.toFile(), folder,
                                 FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, FileFilterUtils.suffixFileFilter( "xml" ) ) );

        Cli c = new Cli();
        c.run( new String[] { "-d", "--settings=" + getClass().getResource( "/settings-test.xml" ).getFile(),
                        "-Dmaven.repo.local=" + folder.toString(), "-l", logfile.getCanonicalPath(),
                        "-DversionSuffix=rebuild-1",
                        "--file",
                        target.getCanonicalPath() } );

        assertFalse( logfile.exists() );
        assertTrue( systemOutRule.getLog().contains( "Disabling log file as running in container" ) );
        assertTrue( systemOutRule.getLog().contains( "Running manipulator" ) );
    }

    @Test
    @BMRule( name = "fake-cgroups-1",
             targetClass = "Cli",
             targetMethod = "getCGroups()",
             targetLocation = "AT ENTRY",
             action = "return org.commonjava.maven.ext.cli.CliLoggingTest.CGROUP_DOCKER"
    )
    public void testLogfileWithDocker() throws Exception
    {
        File folder = temp.newFolder();
        File target = new File( folder, "pom.xml" );
        File logfile = new File( folder, "logfile" );

        FileUtils.copyDirectory( ROOT_DIRECTORY.toFile(), folder,
                                 FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, FileFilterUtils.suffixFileFilter( "xml" ) ) );

        systemOutRule.clearLog();
        Cli c = new Cli();
        c.run( new String[] { "-d", "--settings=" + getClass().getResource( "/settings-test.xml" ).getFile(),
                        "-Dmaven.repo.local=" + folder.toString(), "-l", logfile.getCanonicalPath(),
                        "-DversionSuffix=rebuild-1",
                        "--file",
                        target.getCanonicalPath() } );

        assertFalse( logfile.exists() );
        assertTrue( systemOutRule.getLog().contains( "Disabling log file as running in container" ) );
        assertTrue( systemOutRule.getLog().contains( "Running manipulator" ) );
    }

    @Test
    @BMRule( name = "fake-cgroups-2",
             targetClass = "Cli",
             targetMethod = "getCGroups()",
             targetLocation = "AT ENTRY",
             action = "return org.commonjava.maven.ext.cli.CliLoggingTest.CGROUP_KUBE"
    )
    public void testLogfileWithKubernetes() throws Exception
    {
        File folder = temp.newFolder();
        File target = new File( folder, "pom.xml" );
        File logfile = new File( folder, "logfile" );

        FileUtils.copyDirectory( ROOT_DIRECTORY.toFile(), folder,
                                 FileFilterUtils.or( DirectoryFileFilter.DIRECTORY, FileFilterUtils.suffixFileFilter( "xml" ) ) );

        Cli c = new Cli();
        c.run( new String[] { "-d", "--settings=" + getClass().getResource( "/settings-test.xml" ).getFile(),
                        "-Dmaven.repo.local=" + folder.toString(), "-l", logfile.getCanonicalPath(),
                        "-DversionSuffix=rebuild-1",
                        "--file",
                        target.getCanonicalPath() } );

        assertFalse( logfile.exists() );
        assertTrue( systemOutRule.getLog().contains( "Disabling log file as running in container" ) );
        assertTrue( systemOutRule.getLog().contains( "Running manipulator" ) );
    }
}
