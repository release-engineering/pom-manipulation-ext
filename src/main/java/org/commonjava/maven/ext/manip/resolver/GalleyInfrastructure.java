package org.commonjava.maven.ext.manip.resolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.TransferManagerImpl;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.filearc.FileTransport;
import org.commonjava.maven.galley.filearc.ZipJarTransport;
import org.commonjava.maven.galley.internal.xfer.DownloadHandler;
import org.commonjava.maven.galley.internal.xfer.ExistenceHandler;
import org.commonjava.maven.galley.internal.xfer.ListingHandler;
import org.commonjava.maven.galley.internal.xfer.UploadHandler;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.maven.ArtifactManager;
import org.commonjava.maven.galley.maven.ArtifactMetadataManager;
import org.commonjava.maven.galley.maven.internal.ArtifactManagerImpl;
import org.commonjava.maven.galley.maven.internal.ArtifactMetadataManagerImpl;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMavenPluginImplications;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.internal.version.VersionResolverImpl;
import org.commonjava.maven.galley.maven.model.view.XPathManager;
import org.commonjava.maven.galley.maven.parse.MavenMetadataReader;
import org.commonjava.maven.galley.maven.parse.MavenPomReader;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginImplications;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.maven.spi.version.VersionResolver;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.cache.CacheProvider;
import org.commonjava.maven.galley.spi.event.FileEventManager;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.commonjava.maven.galley.spi.transport.LocationExpander;
import org.commonjava.maven.galley.spi.transport.Transport;
import org.commonjava.maven.galley.spi.transport.TransportManager;
import org.commonjava.maven.galley.transport.TransportManagerImpl;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * Manager component responsible for setting up and managing the Galley API instances used to resolve POMs and metadata.
 * 
 * @author jdcasey
 */
@Component( role = ExtensionInfrastructure.class, hint = "galley" )
public class GalleyInfrastructure
    implements ExtensionInfrastructure
{

    @Requirement
    private org.codehaus.plexus.logging.Logger testLogger;

    private MavenPomReader pomReader;

    private LocationExpander locationExpander;

    private ArtifactManager artifactManager;

    private MavenMetadataReader metadataReader;

    protected GalleyInfrastructure()
    {
    }

    public GalleyInfrastructure( final ManipulationSession session )
        throws ManipulationException
    {
        init( session );
    }

    public GalleyInfrastructure( final ManipulationSession session, final Location customLocation,
                                 final Transport customTransport, final File cacheDir )
        throws ManipulationException
    {
        init( session, customLocation, customTransport, cacheDir );
    }

    public MavenPomReader getPomReader()
    {
        return pomReader;
    }

    @Override
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        init( session, null, null, null );
    }

    private void init( final ManipulationSession session, final Location customLocation,
                       final Transport customTransport, File cacheDir )
        throws ManipulationException
    {
        configureLogging( session );

        try
        {
            final List<Location> custom =
                customLocation == null ? Collections.<Location> emptyList()
                                : Collections.singletonList( customLocation );

            locationExpander =
                new MavenLocationExpander( custom, session.getRemoteRepositories(), session.getLocalRepository() );
        }
        catch ( final MalformedURLException e )
        {
            throw new ManipulationException( "Failed to setup Maven-specific LocationExpander: %s", e, e.getMessage() );
        }

        final XMLInfrastructure xml = new XMLInfrastructure();
        final XPathManager xpaths = new XPathManager();

        final TransportManager transports;
        if ( customTransport != null )
        {
            transports = new TransportManagerImpl( customTransport );
        }
        else
        {
            transports =
                new TransportManagerImpl( new HttpClientTransport( new HttpImpl( new MemoryPasswordManager() ) ),
                                          new FileTransport(), new ZipJarTransport() );
        }

        if ( cacheDir == null )
        {
            cacheDir = new File( session.getTargetDir(), "manipulator-cache" );
        }

        final FileEventManager fileEvents = new NoOpFileEventManager();

        final CacheProvider cache =
            new FileCacheProvider( cacheDir, new HashedLocationPathGenerator(), fileEvents, new NoOpTransferDecorator() );

        final NotFoundCache nfc = new MemoryNotFoundCache();
        final ExecutorService executor = Executors.newCachedThreadPool();

        final TransferManager transfers =
            new TransferManagerImpl( transports, cache, nfc, fileEvents, new DownloadHandler( nfc, executor ),
                                     new UploadHandler( nfc, executor ), new ListingHandler( nfc ),
                                     new ExistenceHandler( nfc ), executor );

        final TypeMapper types = new StandardTypeMapper();
        final ArtifactMetadataManager metadataManager = new ArtifactMetadataManagerImpl( transfers, locationExpander );

        final VersionResolver versionResolver =
            new VersionResolverImpl( new MavenMetadataReader( xml, locationExpander, metadataManager, xpaths ) );

        artifactManager = new ArtifactManagerImpl( transfers, locationExpander, types, versionResolver );

        // TODO: auto-adjust this to the current Maven runtime!
        final MavenPluginDefaults pluginDefaults = new StandardMaven304PluginDefaults();

        final MavenPluginImplications pluginImplications = new StandardMavenPluginImplications( xml );

        pomReader =
            new MavenPomReader( xml, locationExpander, artifactManager, xpaths, pluginDefaults, pluginImplications );

        metadataReader = new MavenMetadataReader( xml, locationExpander, metadataManager, xpaths );
    }

    private void configureLogging( final ManipulationSession session )
    {
        final String userHome = System.getProperty( "user.home" );
        final File logConf = new File( userHome, ".m2/logback.xml" );

        try
        {
            if ( logConf != null && logConf.exists() )
            {
                final String logProps = FileUtils.readFileToString( logConf );

                final JoranConfigurator fig = new JoranConfigurator();
                final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

                final List<Logger> loggerList = context.getLoggerList();
                if ( loggerList != null )
                {
                    for ( final Logger logger : loggerList )
                    {
                        logger.detachAndStopAllAppenders();
                    }
                }

                fig.setContext( context );
                fig.doConfigure( new ByteArrayInputStream( logProps.getBytes() ) );
            }
        }
        catch ( final IOException e )
        {
            testLogger.debug( "Cannot read logback config file: " + logConf, e );
        }
        catch ( final JoranException e )
        {
            testLogger.debug( "Cannot parse logback config file: " + logConf, e );
        }

        if ( testLogger == null || testLogger.isDebugEnabled() )
        {
            final Logger root = (Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
            root.setLevel( Level.DEBUG );
        }
    }

    public MavenMetadataReader getMetadataReader()
    {
        return metadataReader;
    }
}
