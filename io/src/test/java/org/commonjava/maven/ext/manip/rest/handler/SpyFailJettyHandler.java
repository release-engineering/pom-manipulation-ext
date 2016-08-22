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

package org.commonjava.maven.ext.manip.rest.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.ext.manip.rest.mapper.DAMapper;
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
 * Jetty handler that records requests and always fails with HTTP error 504.
 *
 * @author Jakub Senko <jsenko@redhat.com>
 */
@SuppressWarnings( "unchecked" )
public class SpyFailJettyHandler extends AbstractHandler implements Handler
{
    private static final Logger LOGGER = LoggerFactory.getLogger( SpyFailJettyHandler.class );

    private static final String ENDPOINT = "/";

    private static final String METHOD = "POST";

    private static final int ERROR_STATUS_CODE = HttpServletResponse.SC_GATEWAY_TIMEOUT;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<List<Map<String, Object>>> requestData = new ArrayList<>();

    @Override public void handle( String target, Request baseRequest, HttpServletRequest request,
                                  HttpServletResponse response )
                    throws IOException, ServletException
    {

        LOGGER.info( "Handling: {} {}", request.getMethod(), request.getPathInfo() );

        if ( target.equals( ENDPOINT ) && request.getMethod().equals( METHOD ) )
        {
            LOGGER.info( "Handling with SpyFailJettyHandler" );

            // Get Request Body
            StringBuilder jb = new StringBuilder();
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

            List<Map<String, Object>> requestBody;

            // Protocol analysis
            if ( jb.toString().startsWith( "{\"productNames" ))
            {
                DAMapper daMapper = objectMapper.readValue( jb.toString(), DAMapper.class );
                requestBody = daMapper.gavs;
            }
            else
            {
                requestBody = objectMapper.readValue( jb.toString(), List.class );
            }

            requestData.add(requestBody);

            response.setStatus( HttpServletResponse.SC_GATEWAY_TIMEOUT );
            baseRequest.setHandled( true );

        }
        else
        {
            LOGGER.info("Handling with SpyFailJettyHandler failed.");
        }
    }

    public List<List<Map<String, Object>>> getRequestData() {
        return requestData;
    }
}
