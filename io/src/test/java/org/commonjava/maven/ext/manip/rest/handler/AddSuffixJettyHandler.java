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

package org.commonjava.maven.ext.manip.rest.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
@SuppressWarnings( "unchecked" )
public class AddSuffixJettyHandler
                extends AbstractHandler
                implements Handler
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AddSuffixJettyHandler.class );

    private static final String DEFAULT_ENDPOINT = "/";

    private static final String DEFAULT_METHOD = "POST";

    private static final String DEFAULT_SUFFIX = "redhat-1";

    private final String endpoint;

    private final String method;

    private final String suffix;

    private ObjectMapper objectMapper = new ObjectMapper();

    public AddSuffixJettyHandler()
    {
        this( DEFAULT_ENDPOINT, DEFAULT_METHOD, DEFAULT_SUFFIX );
    }

    public AddSuffixJettyHandler( String endpoint, String method, String suffix )
    {
        this.endpoint = endpoint;
        this.method = method;
        this.suffix = suffix;
    }

    @Override public void handle( String target, Request baseRequest, HttpServletRequest request,
                                  HttpServletResponse response )
                    throws IOException, ServletException
    {

        LOGGER.info( "Handling: {} {}", request.getMethod(), request.getPathInfo() );

        if ( target.equals( this.endpoint ) && request.getMethod().equals( this.method ) )
        {
            LOGGER.info( "Handling with AddSuffixJettyHandler" );

            // Get Request Body
            StringBuffer jb = new StringBuffer();
            try
            {
                String line;
                BufferedReader reader = request.getReader();
                while ( ( line = reader.readLine() ) != null )
                {
                    jb.append( line );
                }

            }
            catch ( Exception e )
            {
                LOGGER.warn( "Error reading request body. {}", e.getMessage() );
                return;
            }

            List<Map<String, Object>> requestBody = objectMapper.readValue( jb.toString(), List.class );

            // Prepare Response
            List<Map<String, Object>> responseBody = new ArrayList<Map<String, Object>>();
            for ( Map<String, Object> gav : requestBody)
            {
                String version = (String) gav.get( "version" );
                List<String> availableVersions = new ArrayList<String>();
                String bestMatchVersion = version + "-" + this.suffix;
                availableVersions.add( bestMatchVersion );

                gav.put( "bestMatchVersion", bestMatchVersion );
                gav.put( "whitelisted", false );
                gav.put( "blacklisted", false );
                gav.put( "availableVersions", availableVersions );

                responseBody.add( gav );
            }

            // Set Response
            response.setContentType( "application/json;charset=utf-8" );
            response.setStatus( HttpServletResponse.SC_OK );
            baseRequest.setHandled( true );
            response.getWriter().println( objectMapper.writeValueAsString( responseBody ) );
        }
        else
        {
            LOGGER.info( "Handling: {} {} with AddSuffixJettyHandler failed,"
                                         + " because expected method was {} and endpoint {}", request.getMethod(),
                         request.getPathInfo(), this.method, this.endpoint );
        }
    }
}
