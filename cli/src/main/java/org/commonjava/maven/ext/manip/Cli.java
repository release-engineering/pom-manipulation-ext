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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.SettingsUtils;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.ext.manip.impl.RESTManipulator;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.commonjava.maven.ext.manip.model.SimpleScopedArtifactRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Cli
{
    private static final String PRINT_GAVTC = "printGAVTC";

    public static final File DEFAULT_GLOBAL_SETTINGS_FILE =
        new File( System.getProperty( "maven.home", System.getProperty( "user.dir", "" ) ), "conf/settings.xml" );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    private ManipulationManager manipulationManager;

    private PomIO pomIO;

    /**
     * Default pom file to operate against.
     */
    private File target = new File( System.getProperty( "user.dir" ), "pom.xml" );

    /**
     * Optional settings.xml file.
     */
    private File settings = null;

    /**
     * Properties a user may define on the command line.
     */
    private Properties userProps;

    public static void main( String[] args )
    {
        System.exit ( new Cli().run( args ) );
    }

    public int run( String[] args )
    {
        Options options = new Options();
        options.addOption( "h", false, "Print this help message." );
        options.addOption( Option.builder( "d" ).longOpt( "debug" ).desc( "Enable debug" ).build() );
        options.addOption( Option.builder( "t" ).longOpt( "debug" ).desc( "Enable trace" ).build() );
        options.addOption( Option.builder( "h" ).longOpt( "help" ).desc( "Print help" ).build() );
        options.addOption( Option.builder( "f" )
                                 .longOpt( "file" )
                                 .hasArgs()
                                 .numberOfArgs( 1 )
                                 .desc( "POM file" )
                                 .build() );
        options.addOption( Option.builder( "l" )
                                 .longOpt( "log" )
                                 .desc( "Log file to output logging to" )
                                 .numberOfArgs( 1 )
                                 .build() );
        options.addOption( Option.builder( "s" )
                                 .longOpt( "settings" )
                                 .hasArgs()
                                 .numberOfArgs( 1 )
                                 .desc( "Optional settings.xml file" )
                                 .build() );
        options.addOption( Option.builder( "P" )
                                 .longOpt( "activeProfiles" )
                                 .desc( "Comma separated list of active profiles. Only used with '-p' (Print all project dependencies)" )
                                 .numberOfArgs( 1 )
                                 .build() );
        options.addOption( Option.builder( "o" )
                                 .longOpt( "outputFile" )
                                 .desc( "outputFile to output dependencies to. Only used with '-p' (Print all project dependencies)" )
                                 .numberOfArgs( 1 )
                                 .build() );
        options.addOption( Option.builder( "p" ).longOpt( "printDeps" ).desc( "Print all project dependencies" ).build() );
        options.addOption( Option.builder()
                                 .longOpt( PRINT_GAVTC )
                                 .desc( "Print all project dependencies in group:artifact:version:type:classifier with scope information" )
                                 .build() );
        options.addOption( Option.builder( "D" )
                                 .hasArgs()
                                 .numberOfArgs( 2 )
                                 .valueSeparator( '=' )
                                 .desc( "Java Properties" )
                                 .build() );

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
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
            return 1;
        }

        if ( cmd.hasOption( 'h' ) )
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "...", options );
            System.exit( 1 );
        }
        if ( cmd.hasOption( 'D' ) )
        {
            userProps = cmd.getOptionProperties( "D" );
        }
        if ( cmd.hasOption( 'f' ) )
        {
            target = new File( cmd.getOptionValue( 'f' ) );
        }
        if ( cmd.hasOption( 's' ) )
        {
            settings = new File( cmd.getOptionValue( 's' ) );
        }

        createSession( target, settings );

        final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
        if ( cmd.hasOption( 'l' ) )
        {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();

            PatternLayoutEncoder ple = new PatternLayoutEncoder();
            ple.setPattern( "%level %logger{10} %msg%n" );
            ple.setContext( loggerContext );
            ple.start();

            FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
            fileAppender.setEncoder( ple );
            fileAppender.setContext( loggerContext );
            fileAppender.setName( "fileLogging" );
            fileAppender.setAppend( false );
            fileAppender.setFile( cmd.getOptionValue( "l" ) );
            fileAppender.start();

            root.addAppender( fileAppender );
            root.setLevel( Level.INFO );
        }
        // Set debug logging after session creation else we get the log filled with Plexus
        // creation stuff.
        if ( cmd.hasOption( 'd' ) )
        {
            root.setLevel( Level.DEBUG );
        }
        if ( cmd.hasOption( 't' ) )
        {
            root.setLevel( Level.TRACE );
        }

        if ( !session.isEnabled() )
        {
            logger.info( "Manipulation engine disabled via command-line option" );
            return 1;
        }
        if ( !target.exists() )
        {
            logger.info( "Manipulation engine disabled. No project found." );
            return 1;
        }
        // Don't bother skipping if we're just trying to analyse deps.
        else if ( new File( target.getParentFile(), ManipulationManager.MARKER_FILE ).exists() && !cmd.hasOption( 'p' ) )
        {
            logger.info( "Skipping manipulation as previous execution found." );
            return 0;
        }

        try
        {
            manipulationManager.init( session );

            if ( cmd.hasOption( 'p' ) || cmd.hasOption( PRINT_GAVTC ) )
            {
                Set<String> activeProfiles = null;
                if ( cmd.hasOption( 'P' ) )
                {
                    activeProfiles = new HashSet<String>();
                    Collections.addAll( activeProfiles, cmd.getOptionValue( 'P' ).split( "," ) );
                }
                Set<ArtifactRef> ts = RESTManipulator.establishDependencies( pomIO.parseProject( session.getPom() ), activeProfiles );
                logger.info( "Found {} dependencies.", ts.size() );
                File output = null;

                if ( cmd.hasOption( 'o' ) )
                {
                    output = new File( cmd.getOptionValue( 'o' ) );
                    output.delete();
                }
                for ( ArtifactRef a : ts )
                {
                    String scope = null;
                    if ( a instanceof SimpleScopedArtifactRef )
                    {
                        scope = ( (SimpleScopedArtifactRef) a ).getScope();
                    }
                    if ( cmd.hasOption( 'o' ) )
                    {
                        if ( cmd.hasOption( PRINT_GAVTC ) )
                        {
                            FileUtils.writeStringToFile( output, String.format( "%-80s%10s\n", a, scope ), true );
                        }
                        else
                        {
                            FileUtils.writeStringToFile( output, a.asProjectVersionRef().toString() + '\n', true );
                        }
                    }
                    else
                    {
                        if ( cmd.hasOption( PRINT_GAVTC ) )
                        {
                            System.out.format( "%-80s%10s\n", a, scope );
                        }
                        else
                        {
                            System.out.println( a.asProjectVersionRef() );
                        }
                    }
                }
            }
            else
            {
                manipulationManager.scanAndApply( session );
            }

        }
        catch ( ManipulationException e )
        {
            logger.error( "POM Manipulation failed: Unable to parse projects ", e );
            return 1;
        }
        catch ( Exception e )
        {
            logger.error( "POM Manipulation failed.", e );
            return 1;
        }
        return 0;
    }

    private void createSession( File target, File settings )
    {
        try
        {
            PlexusContainer container = new DefaultPlexusContainer( );

            final MavenExecutionRequest req =
                new DefaultMavenExecutionRequest().setUserProperties( System.getProperties() )
                                                  .setUserProperties( userProps )
                                                  .setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );

            if (userProps != null && userProps.containsKey( "maven.repo.local" ))
            {
                ArtifactRepository ar = new MavenArtifactRepository(  );
                ar.setUrl( "file://" + userProps.getProperty( "maven.repo.local" ));
                req.setLocalRepository( ar );
            }

            if ( settings != null )
            {
                req.setUserSettingsFile( settings );
                req.setGlobalSettingsFile( settings );

                MavenExecutionRequestPopulator executionRequestPopulator =
                    container.lookup( MavenExecutionRequestPopulator.class );
                executionRequestPopulator.populateFromSettings( req, parseSettings( settings ) );
            }

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
        catch ( SettingsBuildingException e )
        {
            logger.debug( "Caught problem parsing settings file ", e );
            System.err.println( "Unable to parse settings.xml file" );
            System.exit( 1 );
        }
        catch ( MavenExecutionRequestPopulationException e )
        {
            logger.debug( "Caught problem populating maven request from settings file ", e );
            System.err.println( "Unable to create maven execution request from settings.xml file" );
            System.exit( 1 );
        }
    }

    private Settings parseSettings( File settings )
        throws PlexusContainerException, ComponentLookupException, SettingsBuildingException
    {
        PlexusContainer container = new DefaultPlexusContainer();
        DefaultSettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
        settingsRequest.setUserSettingsFile( settings );
        settingsRequest.setGlobalSettingsFile( DEFAULT_GLOBAL_SETTINGS_FILE );

        SettingsBuilder settingsBuilder = container.lookup( SettingsBuilder.class );
        SettingsBuildingResult settingsResult = settingsBuilder.build( settingsRequest );
        Settings effectiveSettings = settingsResult.getEffectiveSettings();

        ProfileSelector profileSelector = container.lookup( ProfileSelector.class );
        ProfileActivationContext profileActivationContext =
            new DefaultProfileActivationContext().setActiveProfileIds( effectiveSettings.getActiveProfiles() );
        List<org.apache.maven.model.Profile> modelProfiles = new ArrayList<org.apache.maven.model.Profile>();
        for ( Profile profile : effectiveSettings.getProfiles() )
        {
            modelProfiles.add( SettingsUtils.convertFromSettingsProfile( profile ) );
        }
        List<org.apache.maven.model.Profile> activeModelProfiles =
            profileSelector.getActiveProfiles( modelProfiles, profileActivationContext, new ModelProblemCollector()
            {

                @Override
                public void add( ModelProblem.Severity severity, String message, InputLocation location,
                                 Exception cause )
                {
                    // do nothing
                }
            } );
        List<String> activeProfiles = new ArrayList<String>();
        for ( org.apache.maven.model.Profile profile : activeModelProfiles )
        {
            activeProfiles.add( profile.getId() );
        }
        effectiveSettings.setActiveProfiles( activeProfiles );

        return effectiveSettings;
    }
}
