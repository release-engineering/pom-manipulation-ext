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
package org.commonjava.maven.ext.manip.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang.math.RandomUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ListUtils;
import org.commonjava.maven.ext.manip.rest.exception.ClientException;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.exception.ServerException;
import org.commonjava.maven.ext.manip.rest.mapper.ProjectVersionRefMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * @author vdedik@redhat.com
 */
public class DefaultVersionTranslator
    implements VersionTranslator
{
    private static final Random RANDOM = new Random();

    private final Base32 codec = new Base32( );

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String endpointUrl;

    /**
     * If 0 - no limit.
     * Otherwise loop in batches of maxRestSize.
     */
    private final int maxRestSize;

    public DefaultVersionTranslator( String endpointUrl, int maxRestSize )
    {
        this.endpointUrl = endpointUrl;
        this.maxRestSize = maxRestSize;

        Unirest.setObjectMapper( new ProjectVersionRefMapper() );
        // According to https://github.com/Mashape/unirest-java the default connection timeout is 10000
        // and the default socketTimeout is 60000. We have increased that to 10 minutes.
        Unirest.setTimeouts( 30000, 600000 );
    }

    public Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> allProjects )
    {
        final List<List<ProjectVersionRef>> partition = ListUtils.partition( allProjects, (maxRestSize == 0 ? allProjects.size() : maxRestSize) );
        final Map<ProjectVersionRef, String> result = new HashMap<>();

        // Execute request to get translated versions
        HttpResponse<Map> r;
        String headerContext;

        if ( isNotEmpty (MDC.get("LOG-CONTEXT") ) )
        {
            headerContext = MDC.get( "LOG-CONTEXT" );
        }
        else
        {
            // If we have no MDC PME has been used as the entry point. Dummy one up for DA.
            byte[] randomBytes = new byte[20];
            RANDOM.nextBytes( randomBytes );
            headerContext = "pme-" + codec.encodeAsString( randomBytes );
        }
        logger.debug ("Using log-ctx {} ", headerContext);

        for (List<ProjectVersionRef> projects : partition )
        {
            logger.debug ("REST call batching in {} ", projects.size());
            try
            {
                r = Unirest.post( this.endpointUrl )
                           .header( "accept", "application/json" )
                           .header( "Content-Type", "application/json" )
                           .header( "Log-Context", headerContext)
                           .body( projects )
                           .asObject( Map.class );
            }
            catch ( UnirestException e )
            {
                throw new RestException( String.format( "Request to server '%s' failed. Exception message: %s", this.endpointUrl,
                                                        e.getMessage() ), e );
            }

            // Handle some corner cases (5xx, 4xx)
            if ( r.getStatus() / 100 == 5 )
            {
                throw new ServerException(
                                String.format( "Server at '%s' failed to translate versions. HTTP status code %s.", this.endpointUrl, r.getStatus() ) );
            }
            else if ( r.getStatus() / 100 == 4 )
            {
                throw new ClientException(
                                String.format( "Server at '%s' could not translate versions. HTTP status code %s.", this.endpointUrl, r.getStatus() ) );
            }

            result.putAll( r.getBody() );
        }

        return result;
    }

    public String getEndpointUrl()
    {
        return endpointUrl;
    }
}
