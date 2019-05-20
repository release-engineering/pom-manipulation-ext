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
package org.commonjava.maven.ext.integrationtest;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.ext.cli.Cli;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.integrationtest.invoker.DefaultExecutionParser;
import org.commonjava.maven.ext.integrationtest.invoker.Execution;
import org.commonjava.maven.ext.integrationtest.invoker.ExecutionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author vdedik@redhat.com
 */
public class TestUtils
{
    private static final Logger logger = LoggerFactory.getLogger( TestUtils.class );

    private static final String BUILD_DIR = System.getProperty( "buildDirectory" );

    private static final String MVN_LOCATION = System.getProperty("maven.home");

    private static final String LOCAL_REPO = System.getProperty( "localRepositoryPath" );

    protected static final String IT_LOCATION = BUILD_DIR + "/it-cli";

    protected static final Map<String, String> DEFAULT_MVN_PARAMS = new HashMap<String, String>()
    {{
            put( "maven.repo.local", LOCAL_REPO );
    }};

    private static Pattern p = Pattern.compile( "rest-.*" );
    protected static final List<String> EXCLUDED_FILES = new ArrayList<String>()
    {{
        add( "setup" );
        try ( DirectoryStream<Path> stream = Files.newDirectoryStream( new File( IT_LOCATION ).toPath(),
                                      p -> p.getFileName().toString().startsWith( "rest-" ) ) )
        {
            // Run in a separate test so a Mock server may be started.
            stream.forEach( p -> add( p.getFileName().toString() ) );
        }
        catch ( IOException e )
        {
            logger.error( "Unable to process excluded files", e );
            throw new RuntimeException( "Unable to process excluded files", e );
        }
        add( "groovy-manipulator-first-http" );
    }};

    protected static final Map<String, String> LOCATION_REWRITE = new HashMap<String, String>()
    {{
            put( "simple-numeric-directory-path", "simple-numeric-directory-path/parent" );
    }};

    /**
     * Run the same/similar execution to what invoker plugin would run.
     *
     * @param workingDir - Working directory where the invoker properties are.
     * @param url The URL to either the REST server that manages versions, or HTTP server for static Groovy scripts or null
     * @throws Exception if an error occurs
     */
    public static void runLikeInvoker( String workingDir, String url )
        throws Exception
    {
        ExecutionParser executionParser = new DefaultExecutionParser( DefaultExecutionParser.DEFAULT_HANDLERS );
        Collection<Execution> executions = executionParser.parse( workingDir );

        // Execute
        for ( Execution e : executions )
        {
            // Setup
            runScript( workingDir, "setup" );

            if ( url != null )
            {
                logger.info( "Resetting URL to: {}", url );
                if ( url.matches( ".*[0-9]+$" ) )
                {
                    e.getJavaParams().put( "restURL", url );
                }
                else
                {
                    e.getJavaParams().put( "groovyScripts", url );
                }
            }

            List<String> args = new ArrayList<>();
            args.add( "-s" );
            args.add( getDefaultTestLocation( "settings.xml" ) );
            args.add( "-d" );
            args.add( toFlagsParams( e.getFlags() ) );

            // Run PME-Cli
            Integer cliExitValue = 0;
            if ( !StringUtils.contains( e.getMvnCommand(), "manipulation.disable=true" ) )
            {
                cliExitValue = runCli( args, e.getJavaParams(), e.getLocation() );
            }
            else
            {
                logger.info( "Skipping running the CLI as Maven goals have the Manipulator disabled." );
            }
            // Run Maven
            Map<String, String> mavenParams = new HashMap<>();
            mavenParams.putAll( DEFAULT_MVN_PARAMS );
            mavenParams.putAll( e.getJavaParams() );
            Integer mavenExitValue = runMaven( e.getMvnCommand(), mavenParams, e.getLocation() );

            // Test return codes
            if ( e.isSuccess() )
            {
                if ( cliExitValue != 0 )
                {
                    throw new ManipulationException ( "PME-Cli (running in: " + workingDir + ") exited with a non zero value : " + cliExitValue );
                }
                if ( mavenExitValue != 0 )
                {
                    throw new ManipulationException ( "Maven (running in: " + workingDir + ") exited with a non zero value." + mavenExitValue );
                }
            }
            else
            {
                if ( cliExitValue == 0 && mavenExitValue == 0 )
                {
                    throw new ManipulationException
                                    ( "Exit value of either PME-Cli (" + cliExitValue + ") or Maven (" + mavenExitValue + ") (running in: " + workingDir + ") must be non-zero.");
                }
            }
        }

        // Verify
        runScript( workingDir, "verify" );
    }

    /**
     * Run pom-manipulation-cli via run() with java params (-D arguments) in workingDir directory.
     *
     * @param args - List of additional command line arguments
     * @param params - Map of String keys and String values representing -D arguments
     * @param workingDir - Working directory in which you want the cli to be run.
     * @throws Exception if an error occurs.
     * @return Exit value
     */
    public static Integer runCli( List<String> args, Map<String, String> params, String workingDir ) throws Exception
    {
        ArrayList<String> arguments = new ArrayList<>( args );
        Collections.addAll( arguments, toJavaParams( params ).split( "\\s+" ) );
        boolean argsContainsFile = false;
        for ( String s : args)
        {
            if ( s.startsWith( "--file" ) )
            {
                argsContainsFile = true;
            }
        }
        if ( ! argsContainsFile )
        {
            arguments.add( "--log=" + workingDir + File.separator + "build.log" );
            arguments.add( "--file=" + workingDir + File.separator + "pom.xml" );
        }
        logger.info( "Invoking CLI with {} ", arguments );
        Cli cli = new Cli();
        Integer result = (Integer) executeMethod( cli, "run", new Object[]{arguments.toArray( new String[0] )} );

        // Close unirest client down to prevent any hanging.
        // Unirest.reset();

        // This is a bit of a hack. The CLI, if log-to-file is enabled resets the logging. As we don't fork and run
        // in the same process this means we need to reset it back again. The benefit of not forking is a simpler test
        // harness and it saves time when running the tests.
        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern( "[%t] %level %logger{32} - %msg%n" );
        ple.setContext( loggerContext );
        ple.start();

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setEncoder( ple );
        consoleAppender.setContext( loggerContext );
        consoleAppender.start();

        root.addAppender( consoleAppender );
        root.setLevel( Level.DEBUG );

        return result;
    }

    /**
     * Run maven process with commands and params (-D arguments) in workingDir directory.
     *
     * @param commands - String representing maven command(s), e.g. "clean install".
     * @param params - Map of String keys and values representing -D arguments.
     * @param workingDir - Working directory.
     * @return Exit value
     * @throws Exception if an error occurs
     */
    public static Integer runMaven( String commands, Map<String, String> params, String workingDir )
        throws Exception
    {
        String stringParams = toJavaParams( params );
        String commandMaven = String.format( "mvn %s %s ", commands, stringParams );

        return runCommandAndWait(commandMaven, workingDir, MVN_LOCATION + "/bin");
    }

    /**
     * Run runScript.groovy script in workingDir directory.
     *
     * @param workingDir - Directory with groovy script.
     * @param file - groovy script to run
     * @throws Exception if an error occurs
     */
    private static void runScript( String workingDir, String file )
        throws Exception
    {
        File verify = new File( workingDir + "/" + file + ".groovy" );
        if ( !verify.isFile() )
        {
            return;
        }
        Binding binding = new Binding();
        binding.setVariable( "basedir", workingDir );
        GroovyScriptEngine engine = new GroovyScriptEngine( workingDir );
        engine.run( file + ".groovy", binding );
    }

    /**
     * Get default location of integration test by test name.
     *
     * @param test - Test name.
     * @return Default location of integration test, e.g. ~/pom-manipulation-ext/integration-test/target/it-cli/it-test
     */
    public static String getDefaultTestLocation( String test )
    {
        return String.format( "%s/%s", IT_LOCATION, test );
    }

    /**
     * Convert string parameters in a Map to a String of -D arguments
     *
     * @param params - Map of java parameters
     * @return - String of -D arguments
     */
    private static String toJavaParams( Map<String, String> params )
    {
        if ( params == null )
        {
            return "";
        }

        String stringParams = "";
        for ( String key : params.keySet() )
        {
            stringParams += String.format( "-D%s=%s ", key, params.get( key ) );
        }
        return stringParams;
    }

    /**
     * Convert string parameters in a Map to a String of -- arguments
     *
     * @param params - Map of java parameters
     * @return - String of - arguments
     */
    private static String toFlagsParams( Map<String, String> params )
    {
        if ( params == null )
        {
            return "";
        }

        String stringParams = "";
        for ( String key : params.keySet() )
        {
            stringParams += String.format( "--%s=%s ", key, params.get( key ) );
        }
        return stringParams;
    }

    /**
     * Run command in another process and wait for it to finish.
     *
     * @param command - Command to be run in another process, e.g. "mvn clean install"
     * @param workingDir - Working directory in which to run the command.
     * @param extraPath
     * @return exit value.
     * @throws Exception if an error occurs
     */
    private static Integer runCommandAndWait( String command, String workingDir, String extraPath )
        throws Exception
    {
        String path = System.getenv( "PATH" );
        if (extraPath != null)
        {
            path = extraPath + System.getProperty("path.separator") + path;
        }

        Process proc = Runtime.getRuntime().exec(command, new String[] {"M2_HOME=" + MVN_LOCATION, "PATH=" + path}, new File(workingDir));
        File buildlog = new File(workingDir + "/build.log");

        BufferedReader stdout = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
        BufferedReader stderr = new BufferedReader( new InputStreamReader( proc.getErrorStream() ) );
        PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( buildlog, true ) ) );

        String line = null;
        String errline = null;
        while ( ( line = stdout.readLine() ) != null || ( errline = stderr.readLine() ) != null )
        {
            if ( line != null )
            {
                out.println( line );
            }
            if ( errline != null )
            {
                out.println( errline );
            }
        }

        stdout.close();
        stderr.close();
        out.flush();
        out.close();

        return proc.waitFor();
    }

    /**
     * Loads *.properties file.
     *
     * @param filePath - File path of the *.properties file
     * @return Loaded properties
     */
    public static Properties loadProps( String filePath )
    {
        File propsFile = new File( filePath );
        Properties props = new Properties();
        if ( propsFile.isFile() )
        {
            try
            {
                FileInputStream fis = new FileInputStream( propsFile );
                props.load( fis );
            }
            catch ( Exception e )
            {
                // ignore
            }
        }

        return props;
    }

    public static Map<String, String> propsToMap( Properties props )
    {
        Map<String, String> map = new HashMap<>();
        for ( Object p : props.keySet() )
        {
            map.put( (String) p, props.getProperty( (String) p ) );
        }

        return map;
    }

    /**
     * Executes a method on an object instance.  The name and parameters of
     * the method are specified.  The method will be executed and the value
     * of it returned, even if the method would have private or protected access.
     */
    private static Object executeMethod( Object instance, String name, Object[] params ) throws Exception
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
