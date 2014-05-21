package org.commonjava.maven.ext.manip.fixture;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.InputStream;
import java.io.OutputStream;

import org.commonjava.maven.ext.manip.resolver.MavenLocationExpander;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.spi.transport.DownloadJob;
import org.commonjava.maven.galley.spi.transport.ExistenceJob;
import org.commonjava.maven.galley.spi.transport.ListingJob;
import org.commonjava.maven.galley.spi.transport.PublishJob;
import org.commonjava.maven.galley.spi.transport.Transport;

public class StubTransport
    implements Transport
{

    private final byte[] data;

    public StubTransport( final byte[] data )
    {
        this.data = data;
    }

    @Override
    public DownloadJob createDownloadJob( final ConcreteResource resource, final Transfer transfer,
                                          final int timeoutSeconds )
        throws TransferException
    {
        return new DownloadJob()
        {
            @Override
            public Transfer call()
                throws Exception
            {
                if ( data == null )
                {
                    return null;
                }

                OutputStream out = null;
                final InputStream in = null;

                try
                {
                    transfer.delete( false );
                    out = transfer.openOutputStream( TransferOperation.DOWNLOAD );
                    out.write( data );
                }
                finally
                {
                    closeQuietly( in );
                    closeQuietly( out );
                }

                return transfer;
            }

            @Override
            public TransferException getError()
            {
                return null;
            }
        };
    }

    @Override
    public ExistenceJob createExistenceJob( final ConcreteResource arg0, final int arg1 )
        throws TransferException
    {
        throw new TransferException( "Not implemented in this stub!" );
    }

    @Override
    public ListingJob createListingJob( final ConcreteResource arg0, final int arg1 )
        throws TransferException
    {
        throw new TransferException( "Not implemented in this stub!" );
    }

    @Override
    public PublishJob createPublishJob( final ConcreteResource arg0, final InputStream arg1, final long arg2,
                                        final int arg3 )
        throws TransferException
    {
        throw new TransferException( "Not implemented in this stub!" );
    }

    @Override
    public PublishJob createPublishJob( final ConcreteResource arg0, final InputStream arg1, final long arg2,
                                        final String arg3, final int arg4 )
        throws TransferException
    {
        throw new TransferException( "Not implemented in this stub!" );
    }

    @Override
    public boolean handles( final Location loc )
    {
        return loc.equals( MavenLocationExpander.EXPANSION_TARGET );
    }

}
