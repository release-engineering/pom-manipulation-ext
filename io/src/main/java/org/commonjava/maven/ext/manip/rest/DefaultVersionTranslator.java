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
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.mapper.ProjectVersionRefMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * @author vdedik@redhat.com
 * @author jsenko@redhat.com
 */
public class DefaultVersionTranslator
    implements VersionTranslator
{
    private static final Random RANDOM = new Random();

    private static final Base32 CODEC = new Base32();

    private static final int CHUNK_SPLIT_COUNT = 4;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String endpointUrl;

    public DefaultVersionTranslator( String endpointUrl )
    {
        this.endpointUrl = endpointUrl;
        Unirest.setObjectMapper( new ProjectVersionRefMapper() );
        // According to https://github.com/Mashape/unirest-java the default connection timeout is 10000
        // and the default socketTimeout is 60000.
        // We have increased the first to 30 seconds and the second to 10 minutes.
        Unirest.setTimeouts( 30000, 600000 );
    }

    /**
     * Translate the versions.
     * There may be a lot of them, possibly causing timeouts or other issues.
     * This is mitigated by splitting them into smaller chunks when an error occurs and retrying.
     */
    public Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> projects )
    {
        final Map<ProjectVersionRef, String> result = new HashMap<>();
        final Queue<Task> queue = new ArrayDeque<>();
        queue.add( new Task( projects, endpointUrl ) );

        while ( !queue.isEmpty() )
        {
            Task task = queue.remove();
            task.executeTranslate();
            if ( task.isSuccess() )
            {
                result.putAll( task.getResult() );
            }
            else
            {
                if ( task.canSplit() )
                {
                    if ( task.getStatus() < 0 )
                    {
                        logger.debug ("Caught exception calling server with message {}", task.getException().getMessage());
                    }
                    else
                    {
                        logger.debug ("Did not get status {} but received {}", SC_OK, task.getStatus());
                    }

                    List<Task> tasks = task.split();

                    logger.warn( "Failed to translate versions for task @{}, splitting and retrying. Chunk size was: {} and new chunk size {} in {} segments.",
                                 task.hashCode(), task.getChunkSize(), tasks.get( 0 ).getChunkSize(), tasks.size());
                    queue.addAll( tasks );
                }
                else
                {
                    if ( task.getStatus() > 0 )
                    {
                        throw new RestException(
                                        "Cannot split and retry anymore. Last status was " + task.getStatus() );
                    }
                    else
                    {
                        throw new RestException( "Cannot split and retry anymore. Cause: " + task.getException(),
                                                 task.getException() );
                    }
                }
            }
        }
        return result;
    }

    private static class Task
    {
        private List<ProjectVersionRef> chunk;

        private Map<ProjectVersionRef, String> result = null;

        private int status = -1;

        private Exception exception;

        private String endpointUrl;

        Task( List<ProjectVersionRef> chunk, String endpointUrl )
        {
            this.chunk = chunk;
            this.endpointUrl = endpointUrl;
        }

        void executeTranslate()
        {
            HttpResponse<Map> r;
            String headerContext;

            if ( isNotEmpty( MDC.get( "LOG-CONTEXT" ) ) )
            {
                headerContext = MDC.get( "LOG-CONTEXT" );
            }
            else
            {
                // If we have no MDC PME has been used as the entry point. Dummy one up for DA.
                byte[] randomBytes = new byte[20];
                RANDOM.nextBytes( randomBytes );
                headerContext = "pme-" + CODEC.encodeAsString( randomBytes );
            }
            try
            {
                r = Unirest.post( this.endpointUrl )
                           .header( "accept", "application/json" )
                           .header( "Content-Type", "application/json" )
                           .header( "Log-Context", headerContext )
                           .body( chunk )
                           .asObject( Map.class );

                status = r.getStatus();
                if ( status == SC_OK )
                {
                    this.result = r.getBody();
                }
            }
            catch ( UnirestException e )
            {
                exception = new RestException(
                                String.format( "Request to server '%s' failed. Exception message: %s", this.endpointUrl,
                                               e.getMessage() ), e );
                this.status = -1;
            }
        }

        public List<Task> split()
        {
            if ( !canSplit() )
            {
                throw new IllegalArgumentException( "Can't split anymore!" );
            }
            List<Task> res = new ArrayList<>( CHUNK_SPLIT_COUNT );
            // To KISS, overflow the remainder into the last chunk
            int chunkSize = chunk.size() / CHUNK_SPLIT_COUNT;
            for ( int i = 0; i < ( CHUNK_SPLIT_COUNT - 1 ); i++ )
            {
                res.add( new Task( chunk.subList( i * chunkSize, ( i + 1 ) * chunkSize ), endpointUrl ) );
            }
            // Last chunk may have different size
            res.add( new Task( chunk.subList( ( CHUNK_SPLIT_COUNT - 1 ) * chunkSize, chunk.size() ), endpointUrl ) );
            return res;
        }

        boolean canSplit()
        {
            return ( chunk.size() / CHUNK_SPLIT_COUNT ) > 0;
        }

        int getStatus()
        {
            return status;
        }

        boolean isSuccess()
        {
            return status == SC_OK;
        }

        public Map<ProjectVersionRef, String> getResult()
        {
            return result;
        }

        public Exception getException()
        {
            return exception;
        }

        int getChunkSize()
        {
            return chunk.size();
        }
    }
}
