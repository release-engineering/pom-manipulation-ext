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
package org.commonjava.maven.ext.manip;

import ch.qos.logback.classic.Level;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.Properties;

public class Cli
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    private ManipulationManager manipulationManager;

    private PomIO pomIO;

    /**
     * Default pom file to operate against.
     */
    private File target = new File( System.getProperty( "user.dir" ), "pom.xml" );

    /**
     * Properties a user may define on the command line.
     */
    private Properties userProps;

    public static void main( String[] args )
    {
        new Cli().run( args );
    }


    private void run( String[] args )
    {
        Options options = new Options();
        options.addOption( "h", false, "Print this help message." );
        options.addOption( Option.builder( "d" )
                                 .longOpt( "debug" )
                                 .desc( "Enable debug" )
                                 .build() );
        options.addOption( Option.builder( "h" )
                                 .longOpt( "help" )
                                 .desc( "Print help" )
                                 .build() );
        options.addOption( Option.builder( "f" )
                                 .longOpt( "file" )
                                 .hasArgs()
                                 .numberOfArgs( 1 )
                                 .desc( "POM file" )
                                 .build() );
        options.addOption( Option.builder( "D" )
                                 .hasArgs()
                                 .numberOfArgs( 2 )
                                 .valueSeparator( '=' )
                                 .desc( "Java Properties" )
                                 .build() );

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try
        {
            cmd = parser.parse( options, args );
        }
        catch ( ParseException e )
        {
            logger.debug( "Caught problem parsing ", e );
            System.err.println( e.getMessage() );

            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "...", options );
            System.exit( 1 );

        }

        if (cmd.hasOption( 'h' ))
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "...", options );
            System.exit( 1 );
        }
        if (cmd.hasOption( 'D' ))
        {
            userProps = cmd.getOptionProperties( "D" );
        }
        if (cmd.hasOption( 'f' ))
        {
            target = new File (cmd.getOptionValue( 'f' ));
        }

        createSession( target );

        // Set debug logging after session creation else we get the log filled with Plexus
        // creation stuff.
        if (cmd.hasOption( 'd' ))
        {
            final ch.qos.logback.classic.Logger root =
                            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
            root.setLevel( Level.DEBUG );
        }

        if ( !session.isEnabled() )
        {
            logger.info( "Manipulation engine disabled via command-line option" );
            return;
        }
        if ( !target.exists() )
        {
            logger.info( "Manipulation engine disabled. No project found." );
            return;
        }
        else if ( new File( target, ManipulationManager.MARKER_FILE ).exists() )
        {
            logger.info( "Skipping manipulation as previous execution found." );
            return;
        }

        try
        {
            manipulationManager.init( session );
            manipulationManager.scanAndApply( session );
        }
        catch ( ManipulationException e )
        {
            logger.error( "Unable to parse projects ", e );
            System.exit( 1 );
        }
    }

    private void createSession( File target )
    {
        try
        {
            PlexusContainer container = new DefaultPlexusContainer( );

            final MavenExecutionRequest req =
                            new DefaultMavenExecutionRequest().setUserProperties( System.getProperties() )
                                                              .setUserProperties( userProps )
                                                              .setRemoteRepositories(
                                                                              Collections.<ArtifactRepository>emptyList() );
            final MavenSession mavenSession =
                            new MavenSession( container, null, req, new DefaultMavenExecutionResult() );

            mavenSession.getRequest().setPom( target );

            pomIO = container.lookup( PomIO.class );
            session = container.lookup( ManipulationSession.class );
            manipulationManager = container.lookup( ManipulationManager.class );

            session.setMavenSession( mavenSession );
        }
        catch ( ComponentLookupException e )
        {
            logger.debug( "Caught problem instantiating ", e );
            System.err.println( "Unable to start Cli subsystem" );
            System.exit( 1 );
            e.printStackTrace();
        }
        catch ( PlexusContainerException e )
        {
            logger.debug( "Caught problem instantiating ", e );
            System.err.println( "Unable to start Cli subsystem" );
            System.exit( 1 );
        }
    }
}
