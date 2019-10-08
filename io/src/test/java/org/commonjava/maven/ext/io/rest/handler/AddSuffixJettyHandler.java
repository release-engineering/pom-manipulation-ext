/*
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
package org.commonjava.maven.ext.io.rest.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.ext.common.util.JSONUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.jboss.da.model.rest.GAV;
import org.jboss.da.reports.model.request.LookupGAVsRequest;
import org.jboss.da.reports.model.response.LookupReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * @author vdedik@redhat.com
 */
public class AddSuffixJettyHandler
                extends AbstractHandler
                implements Handler
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AddSuffixJettyHandler.class );

    private static final String DEFAULT_ENDPOINT = "/reports/lookup/gavs";

    public static final String SUFFIX = "redhat";

    public static final String DEFAULT_SUFFIX = SUFFIX + "-1";

    public static final String EXTENDED_SUFFIX = SUFFIX + "-2";

    public static final String MIXED_SUFFIX = "temporary-" + DEFAULT_SUFFIX;

    public static final String TIMESTAMP_SUFFIX = "t20180920-163311-423-" + DEFAULT_SUFFIX;

    private final String endpoint;

    private String suffix;

    private JSONUtils.InternalObjectMapper objectMapper = new JSONUtils.InternalObjectMapper( new ObjectMapper(  ));

    private String blacklistVersion = null;

    public AddSuffixJettyHandler()
    {
        this( DEFAULT_ENDPOINT, DEFAULT_SUFFIX );
    }

    public AddSuffixJettyHandler( String endpoint, String suffix )
    {
        this.endpoint = endpoint;
        this.suffix = suffix;
    }

    @Override
    public void handle( String target, Request baseRequest, HttpServletRequest request,
                                  HttpServletResponse response )
                    throws IOException
    {
        LOGGER.info( "Handling with AddSuffixJettyHandler: {} {}", request.getMethod(), request.getPathInfo() );

        if ( target.startsWith( this.endpoint ) )
        {
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
            LOGGER.info( "Read request body '{}' and read parameters '{}'.", jb , request.getParameterMap() );

            List<GAV> requestBody;

            if ( target.equals( DEFAULT_ENDPOINT ) )
            {
                List<LookupReport> responseBody = new ArrayList<>();

                // Protocol analysis
                LookupGAVsRequest lookupGAVsRequest = objectMapper.readValue( jb.toString(), LookupGAVsRequest.class );
                requestBody = lookupGAVsRequest.getGavs();

                boolean useCustomMixedSuffix = requestBody.stream().anyMatch( r -> r.getArtifactId().equals( "rest-version-manip-mixed-suffix-orig-rh" ) );
                boolean usePartialCustomMixedSuffix = requestBody.stream().anyMatch( r -> r.getArtifactId().equals( "rest-version-manip-mixed-suffix-orig-rh-norhalign" ) );

                // Prepare Response
                for ( GAV gav : requestBody )
                {
                    LookupReport lr = new LookupReport( gav );
                    List<String> availableVersions = new ArrayList<>();

                    String version = gav.getVersion();
                    String bestMatchVersion;

                    LOGGER.debug( "Processing artifactId {} with version {}", gav.getArtifactId(), version );

                    // Specific to certain integration tests. For the SNAPSHOT test we want to verify it can handle
                    // a already built version. The PME code should remove SNAPSHOT before sending it.
                    if ( gav.getArtifactId().startsWith( "rest-dependency-version-manip-child-module" ) )
                    {
                        bestMatchVersion = version + "-" + EXTENDED_SUFFIX;
                    }
                    else if ( gav.getGroupId().equals( "org.goots.maven.circulardependencies-test-parent" ) )
                    {
                        bestMatchVersion = version + "." + SUFFIX + "-3";
                    }
                    else if ( gav.getArtifactId().equals( "rest-version-manip-mixed-suffix-orig-rh" ) )
                    {
                        bestMatchVersion = version + "-" + this.suffix;
                    }
                    else if ( gav.getArtifactId().startsWith( "depMgmt2" ) ||
                                    gav.getArtifactId().startsWith( "pluginMgmt3" ) )
                    {
                        bestMatchVersion = "1.0.0-" + EXTENDED_SUFFIX;
                    }
                    else if ( gav.getArtifactId().startsWith( "rest-version-manip-suffix-strip-increment" ) )
                    {
                        if ( version.contains( "jbossorg" ) )
                        {
                            bestMatchVersion = "";
                        }
                        else
                        {
                            bestMatchVersion = version + "-" + EXTENDED_SUFFIX;
                        }
                    }
                    else if ( useCustomMixedSuffix || usePartialCustomMixedSuffix)
                    {
                        if ( useCustomMixedSuffix && gav.getArtifactId().equals( "commons-lang" ) )
                        {
                            // We know its redhat-1 (hardcoded in the pom).
                            int separatorIndex = version.indexOf( SUFFIX ) - 1;
                            bestMatchVersion =
                                            version.substring( 0, separatorIndex ) + version.substring( separatorIndex,
                                                                                                        separatorIndex + 1 )
                                                            + MIXED_SUFFIX;
                        }
                        else if ( useCustomMixedSuffix && gav.getArtifactId().equals( "errai-tools" ) )
                        {
                            int separatorIndex = version.indexOf( SUFFIX ) - 1;
                            bestMatchVersion =
                                            version.substring( 0, separatorIndex ) + version.substring( separatorIndex,
                                                                                                           separatorIndex + 1 )
                                            + EXTENDED_SUFFIX;
                        }
                        else if ( useCustomMixedSuffix && gav.getArtifactId().equals( "commons-httpclient" ) )
                        {
                            int separatorIndex = version.indexOf( "temporary-redhat" ) - 1;
                            bestMatchVersion =
                                            version.substring( 0, separatorIndex ) + version.substring( separatorIndex,
                                                                                                           separatorIndex + 1 )
                                            + "temporary-redhat-2";
                        }
                        else
                        {
                            bestMatchVersion = version + "-" + MIXED_SUFFIX;
                        }
                    }
                    else
                    {
                        bestMatchVersion = version + "-" + this.suffix;
                    }
                    LOGGER.info( "For GA {}, requesting version {} and got bestMatch {} with availableVersions {} ", gav, version,
                                 bestMatchVersion, availableVersions );

                    availableVersions.add( bestMatchVersion );

                    lr.setBestMatchVersion( bestMatchVersion );
                    lr.setBlacklisted( false );
                    lr.setAvailableVersions( availableVersions );

                    responseBody.add( lr );
                }

                LOGGER.info( "Returning response body size {}", responseBody.size() );

                response.getWriter().println( objectMapper.writeValue( responseBody ) );
            }
            else
            {
                Map<String, Object> gav = new HashMap<>(  );
                gav.put ("groupId", request.getParameter( "groupid" ));
                gav.put ("artifactId", request.getParameter( "artifactid" ));
                if ( isEmpty ( blacklistVersion ) )
                {
                    gav.put( "version", "1.0" + '.' + suffix );
                }
                else
                {
                    gav.put ( "version", blacklistVersion );
                }

                response.getWriter().println( objectMapper.writeValue( Collections.singletonList( gav ) ) );
            }

            // Set Response
            response.setContentType( "application/json;charset=utf-8" );
            response.setStatus( HttpServletResponse.SC_OK );
            baseRequest.setHandled( true );
        }
        else
        {
            LOGGER.info( "Handling: {} with AddSuffixJettyHandler failed, because expected method was {} and endpoint {}",
                         request.getMethod(), request.getPathInfo(), this.endpoint );
        }
    }

    public void setBlacklist( String s )
    {
        blacklistVersion = s;
    }

    public void setSuffix( String suffix )
    {
        this.suffix = suffix;
    }

}