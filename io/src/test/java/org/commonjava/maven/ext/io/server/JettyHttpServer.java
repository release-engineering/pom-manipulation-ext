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
package org.commonjava.maven.ext.io.server;

import org.commonjava.maven.ext.io.server.exception.ServerInternalException;
import org.commonjava.maven.ext.io.server.exception.ServerSetupException;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author vdedik@redhat.com
 */
public class JettyHttpServer
        implements HttpServer
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Integer port;

    private final Server jettyServer;

    private final Handler handler;

    public JettyHttpServer( Handler handler )
    {
        this.handler = handler;
        this.jettyServer = createAndStartJetty();
    }

    public Integer getPort()
    {
        return this.port;
    }

    public void shutdown()
    {
        try
        {
            this.jettyServer.stop();
        }
        catch ( Exception e )
        {
            throw new ServerInternalException( "Error shutting down jetty", e );
        }
    }

    private Server createAndStartJetty()
    {
        final AtomicReference<Integer> foundPort = new AtomicReference<>();
        Server jetty = PortFinder.findPortFor
                        ( 16, p -> {
                            logger.debug( "Creating jetty server on port {} for {}", p, handler );
                            foundPort.set( p );
                            Server result = new Server( new InetSocketAddress( "127.0.0.1", p ) );
                            result.setHandler( handler );
                            try
                            {
                                result.start();
                            }
                            catch ( Exception e )
                            {
                                throw new ServerSetupException( "Error starting jetty on port " + p, e );
                            }
                            return result;
                        });

        this.port = foundPort.get();

        return jetty;
    }
}
