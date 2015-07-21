package org.commonjava.maven.ext.manip;

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
import org.apache.maven.mae.MAEException;
import org.apache.maven.mae.app.AbstractMAEApplication;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

@Component( role = Cli.class )
public class Cli extends AbstractMAEApplication
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private PlexusContainer container;

    @Requirement
    private ManipulationSession session;

    @Requirement
    private ManipulationManager manipulationManager;

    @Requirement
    private PomIO pomIO;

    private File target = new File( System.getProperty( "user.dir" ), "pom.xml" );

    private Properties userProps;

    public static void main( String[] args )
    {
        Cli pmeCli = new Cli();
        try
        {
            pmeCli.load();
        }
        catch ( MAEException e )
        {
            pmeCli.logger.debug( "Caught problem instantiating ", e );
            System.err.println( "Unable to start Cli subsystem" );
            System.exit( 1 );
        }

        pmeCli.run( args );
    }

    private void run( String[] args )
    {
        Options options = new Options();
        options.addOption( "h", false, "Print this help message." );
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

        System.out.println( "### Options " + parser.toString() );
        System.out.println( "### Args " + Arrays.toString( cmd.getOptions() ) );
        System.out.println( "### Args " + Arrays.toString( cmd.getArgs() ) );
        System.out.println( "### Args " + cmd.getArgList() );
        System.out.println( "### Properties " + cmd.getOptionProperties( "D" ) );

        if (cmd.hasOption( 'D' ))
        {
            userProps = cmd.getOptionProperties( "D" );
        }
        if (cmd.hasOption( 'f' ))
        {
            target = new File (cmd.getOptionValue( 'f' ));
        }

        createSession(target);

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

            logger.info( "### ee.getSession().getRequest().getPom() " + session.getTargetDir() + " and target "
                                         + target );
            logger.info ("### session.gettargetdir " + session.getPom() + " and target " + target);

            manipulationManager.scanAndApply( session );
        }
        catch ( ManipulationException e )
        {
            logger.error( "Unable to parse projects ", e );
            System.exit( 1 );
        }
    }

    @Override
    public String getId()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "POM Manipulation Extension for Maven " + getClass().getPackage().getImplementationVersion();
    }

    private void createSession( File target )
    {
        final MavenExecutionRequest req = new DefaultMavenExecutionRequest().setUserProperties( System.getProperties() )
                                                                            .setUserProperties( userProps )
                                                                            .setRemoteRepositories(
                                                                                            Collections.<ArtifactRepository>emptyList() );
        final MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );
        mavenSession.getRequest().setPom( target );

        session.setMavenSession( mavenSession );
    }
/*

    @Override
    protected void configureBuilder( final MAEEmbedderBuilder builder )
        throws MAEException
    {
        super.configureBuilder( builder );
    }
    @Override
    public ComponentSelector getComponentSelector()
    {
        return new ComponentSelector().setSelection( SessionInitializer.class, "vman" );
    }
    */
}
