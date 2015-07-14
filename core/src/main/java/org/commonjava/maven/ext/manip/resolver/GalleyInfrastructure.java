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
package org.commonjava.maven.ext.manip.resolver;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.maven.repository.MirrorSelector;
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
    private MirrorSelector mirrorSelector;

    private MavenPomReader pomReader;

    private LocationExpander locationExpander;

    private ArtifactManager artifactManager;

    private MavenMetadataReader metadataReader;

    private XMLInfrastructure xml;

    private XPathManager xpaths;

    protected GalleyInfrastructure()
    {
    }

    public GalleyInfrastructure( final ManipulationSession session )
        throws ManipulationException
    {
        init( session );
    }

    public GalleyInfrastructure( final ManipulationSession session, final MirrorSelector mirrorSelector,
                                 final Location customLocation, final Transport customTransport, final File cacheDir )
        throws ManipulationException
    {
        this.mirrorSelector = mirrorSelector;
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
        try
        {
            final List<Location> custom =
                customLocation == null ? Collections.<Location> emptyList()
                                : Collections.singletonList( customLocation );

            locationExpander =
                new MavenLocationExpander( custom, session.getRemoteRepositories(), session.getLocalRepository(),
                                           mirrorSelector, session.getSettings(), session.getActiveProfiles() );
        }
        catch ( final MalformedURLException e )
        {
            throw new ManipulationException( "Failed to setup Maven-specific LocationExpander: %s", e, e.getMessage() );
        }

        xml = new XMLInfrastructure();
        xpaths = new XPathManager();

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

    public XMLInfrastructure getXml()
    {
        return xml;
    }

    public MavenMetadataReader getMetadataReader()
    {
        return metadataReader;
    }

    public ArtifactManager getArtifactManager()
    {
        return artifactManager;
    }

    public XPathManager getXPath()
    {
        return xpaths;
    }
}
