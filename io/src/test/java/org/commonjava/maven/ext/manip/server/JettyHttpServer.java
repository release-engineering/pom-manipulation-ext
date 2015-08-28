/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.commonjava.maven.ext.manip.server;

import org.commonjava.maven.ext.manip.server.exception.ServerInternalException;
import org.commonjava.maven.ext.manip.server.exception.ServerSetupException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author vdedik@redhat.com
 */
public class JettyHttpServer
    implements HttpServer
{
    public static final Integer DEFAULT_PORT = 8089;

    private Integer port;

    private final Server jettyServer;

    private Handler handler;

    public JettyHttpServer( Handler handler )
    {
        this( handler, DEFAULT_PORT );
    }

    public JettyHttpServer( Handler handler, Integer port )
    {
        this.port = port;
        this.handler = handler;
        this.jettyServer = createAndStartJetty( port );
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

    private Server createAndStartJetty( Integer port )
    {
        Server jetty = new Server();
        Connector conn = new SelectChannelConnector();
        conn.setHost( "127.0.0.1" );
        conn.setPort( this.port );
        jetty.addConnector( conn );
        jetty.setHandler( handler );

        try
        {
            jetty.start();
        }
        catch ( Exception e )
        {
            throw new ServerSetupException( "Error starting jetty on port " + port, e );
        }

        return jetty;
    }
}
