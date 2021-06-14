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
package org.commonjava.maven.ext.io.rest;

import kong.unirest.GenericType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.http.HttpStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.json.ErrorMessage;
import org.commonjava.maven.ext.common.json.ExtendedMavenLookupResult;
import org.commonjava.maven.ext.common.util.GAVUtils;
import org.commonjava.maven.ext.common.util.JSONUtils.InternalObjectMapper;
import org.commonjava.maven.ext.common.util.ListUtils;
import org.jboss.da.lookup.model.MavenLookupRequest;
import org.jboss.da.lookup.model.MavenLookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * @author ncross@redhat.com
 * @author vdedik@redhat.com
 * @author jsenko@redhat.com
 */
public class DefaultTranslator
    implements Translator
{
    private static final GenericType<List<MavenLookupResult>> lookupType = new GenericType<List<MavenLookupResult>>()
    {
    };
    private static final GenericType<List<ProjectVersionRef>> pvrTyoe = new GenericType<List<ProjectVersionRef>>()
    {
    };

    private static final String LOOKUP_GAVS = "lookup/maven";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String endpointUrl;

    private final int initialRestMaxSize;

    private final int initialRestMinSize;

    private final Boolean brewPullActive;

    private final String mode;

    private final Map<String, String> restHeaders;

    private final int retryDuration;

    private final int restConnectionTimeout;

    private final int restSocketTimeout;

    static
    {
        // According to https://kong.github.io/unirest-java/#configuration the default connection timeout is 10000
        // and the default socketTimeout is 60000.
        // We have increased the first to 30 seconds and the second to 10 minutes.
        Unirest.config()
               .socketTimeout( 600000 )
               .connectTimeout( 30000 )
               .setObjectMapper( new InternalObjectMapper( new com.fasterxml.jackson.databind.ObjectMapper() ) );
    }

    /**
     * @param endpointUrl is the URL to talk to.
     * @param restMaxSize initial (maximum) size of the rest call; if zero will send everything.
     * @param restMinSize minimum size for the call
     * @param brewPullActive flag saying if brew pull should be used for version retrieval
     * @param mode lookup mode, either STANDARD (default if empty) or SERVICE
     * @param restHeaders the headers to pass to the endpoint
     */
    public DefaultTranslator( String endpointUrl, int restMaxSize, int restMinSize, Boolean brewPullActive, String mode,
                              Map<String, String> restHeaders, int restConnectionTimeout, int restSocketTimeout,
                              int restRetryDuration )
    {
        this.brewPullActive = brewPullActive;
        this.mode = mode;
        this.endpointUrl = endpointUrl + ( isNotBlank( endpointUrl ) ? endpointUrl.endsWith( "/" ) ? "" : "/" : "");
        this.initialRestMaxSize = restMaxSize;
        this.initialRestMinSize = restMinSize;
        this.restHeaders = restHeaders;
        this.restConnectionTimeout = restConnectionTimeout;
        this.restSocketTimeout = restSocketTimeout;
        this.retryDuration = restRetryDuration;
    }


    private void partition(List<ProjectVersionRef> projects, Queue<Task> queue) {
        if ( initialRestMaxSize != 0 )
        {
            if (initialRestMaxSize == -1)
            {
                autoPartition(projects, queue);
            }
            else
            {
                userDefinedPartition(projects, queue);
            }
        }
        else
        {
            noOpPartition(projects, queue);
        }
    }

    private void noOpPartition(List<ProjectVersionRef> projects, Queue<Task> queue) {
        logger.info("Using NO-OP partition strategy");

        queue.add(new Task( projects, endpointUrl + LOOKUP_GAVS ));
    }

    private void userDefinedPartition(List<ProjectVersionRef> projects, Queue<Task> queue) {
        logger.info("Using user defined partition strategy");

        // Presplit
        final List<List<ProjectVersionRef>> partition = ListUtils.partition( projects, initialRestMaxSize );

        for ( List<ProjectVersionRef> p : partition )
        {
            queue.add( new Task( p, endpointUrl + LOOKUP_GAVS ) );
        }

        logger.debug( "For initial sizing of {} have split the queue into {} ", initialRestMaxSize , queue.size() );
    }

    private void autoPartition(List<ProjectVersionRef> projects, Queue<Task> queue) {
        List<List<ProjectVersionRef>> partition;

        if (projects.size() < 600)
        {
            logger.info("Using auto partition strategy: {} projects divided in chunks with {} each", projects.size(), 128);
            partition = ListUtils.partition( projects, 128 );
        }
        else {
            if (projects.size() > 600 && projects.size() < 1200)
            {
                logger.info("Using auto partition strategy: {} projects divided in chunks with {} each", projects.size(), 64);
                partition = ListUtils.partition( projects, 64 );
            }
            else
            {
                logger.info("Using auto partition strategy: {} projects divided in chunks with {} each", projects.size(), 32);
                partition = ListUtils.partition( projects, 32 );
            }
        }

        for ( List<ProjectVersionRef> p : partition )
        {
            queue.add( new Task( p, endpointUrl + LOOKUP_GAVS ) );
        }
    }

    @Override
    public Map<ProjectVersionRef, String>  lookupProjectVersions( List<ProjectVersionRef> projects ) throws RestException
    {
        // TODO: ### Implement
        return lookupVersions( projects );
    }

//    @Override
//    public List<ProjectVersionRef> findBlacklisted( ProjectRef ga ) throws RestException
//    {
//        final String blacklistEndpointUrl = endpointUrl + LISTING_BLACKLIST_GA;
//        final AtomicReference<List<ProjectVersionRef>> result = new AtomicReference<>();
//        final String[] errorString = new String[1];
//        final HttpResponse<List<ProjectVersionRef>> r;
//
//        logger.debug( "Called findBlacklisted to {} with {} and custom headers {}", blacklistEndpointUrl, ga, restHeaders );
//
//        try
//        {
//            r = Unirest.get( blacklistEndpointUrl )
//                       .header( "accept", "application/json" )
//                       .header( "Content-Type", "application/json" )
//                       .headers( restHeaders )
//                       .connectTimeout(restConnectionTimeout * 1000)
//                       .socketTimeout(restSocketTimeout * 1000)
//                       .queryString( "groupid", ga.getGroupId() )
//                       .queryString( "artifactid", ga.getArtifactId() )
//                       .asObject( pvrTyoe )
//                       .ifSuccess( successResponse -> result.set( successResponse.getBody() ) )
//                       .ifFailure( failedResponse -> {
//                           if ( !failedResponse.getParsingError().isPresent() )
//                           {
//                               logger.debug( "Parsing error but no message. Status text {}", failedResponse.getStatusText() );
//                               throw new ManipulationUncheckedException( failedResponse.getStatusText() );
//                           }
//                           else
//                           {
//                               String originalBody = failedResponse.getParsingError().get().getOriginalBody();
//
//                               if ( originalBody.length() == 0 )
//                               {
//                                   errorString[0] = "No content to read.";
//                               }
//                               else if ( originalBody.startsWith( "<" ) )
//                               {
//                                   // Read an HTML string.
//                                   String stripped = originalBody.replaceAll( "<.*?>", "" ).replaceAll( "\n", " " ).trim();
//                                   logger.debug( "Read HTML string '{}' rather than a JSON stream; stripping message to '{}'",
//                                                 originalBody, stripped );
//                                   errorString[0] = stripped;
//                               }
//                               else if ( originalBody.startsWith( "{\"" ) )
//                               {
//                                   errorString[0] = failedResponse.mapError( ErrorMessage.class ).toString();
//
//                                   logger.debug( "Read message string {}, processed to {} ", originalBody, errorString );
//                               }
//                               else
//                               {
//                                   throw new ManipulationUncheckedException( "Problem in HTTP communication with status code {} and message {}",
//                                                                             failedResponse.getStatus(), failedResponse.getStatusText() );
//                               }
//                           }
//                       } );
//
//            if ( !r.isSuccess() )
//            {
//                throw new RestException( "Failed to establish blacklist calling {} with error {}", endpointUrl, errorString[0] );
//            }
//        }
//        catch ( ManipulationUncheckedException | UnirestException e )
//        {
//            throw new RestException( "Unable to contact DA", e );
//        }
//
//        return result.get();
//    }



    /**
     * Translate the versions.
     * <pre>
     * [
     *   {
     *     "groupId": "com.google.guava",
     *     "artifactId": "guava",
     *     "version": "13.0.1"
     *   }
     * ]
     * </pre>
     * This equates to a List of ProjectVersionRef.
     *
     * <pre>
     * {
     *     "productNames": [],
     *     "productVersionIds": [],
     *     "repositoryGroup": "",
     *     "gavs": [
     *       {
     *         "groupId": "com.google.guava",
     *         "artifactId": "guava",
     *         "version": "13.0.1"
     *       }
     *     ]
     * }
     * </pre>
     * There may be a lot of them, possibly causing timeouts or other issues.
     * This is mitigated by splitting them into smaller chunks when an error occurs and retrying.
     */
    @Override
    public Map<ProjectVersionRef, String> lookupVersions( List<ProjectVersionRef> p ) throws RestException
    {
        final List<ProjectVersionRef> projects = p.stream().distinct().collect( Collectors.toList() );
        if ( p.size() != projects.size() )
        {
            logger.debug( "Eliminating duplicates from {} resulting in {}", p, projects );
        }
        logger.info( "Calling REST client... (with {} GAVs)", projects.size() );

        final Queue<Task> queue = new ArrayDeque<>();
        final Map<ProjectVersionRef, String> result = new HashMap<>();
        final long start = System.nanoTime();

        boolean finishedSuccessfully = false;

        try
        {
            partition( projects, queue );

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
                    if ( task.canSplit() && isRecoverable( task.getStatus() ) )
                    {
                        if ( task.getStatus() == HttpStatus.SC_SERVICE_UNAVAILABLE )
                        {
                            logger.info( "The DA server is unavailable. Waiting {} before splitting the tasks and retrying",
                                         retryDuration );

                            waitBeforeRetry( retryDuration );
                        }

                        List<Task> tasks = task.split();

                        logger.warn( "Failed to translate versions for task @{} due to {}, splitting and retrying. Chunk size was: {} and new chunk size {} in {} segments.",
                                     task.hashCode(), task.getStatus(), task.getChunkSize(), tasks.get( 0 ).getChunkSize(),
                                     tasks.size() );
                        queue.addAll( tasks );
                    }
                    else
                    {
                        if ( task.getStatus() < 0 )
                        {
                            logger.debug( "Caught exception calling server with message {}", task.getErrorMessage() );
                        }
                        else
                        {
                            logger.debug( "Did not get status {} but received {}", SC_OK, task.getStatus() );
                        }

                        throw new RestException( "Received response status {} with message: {}",
                                                 task.getStatus(), task.getErrorMessage() );
                    }
                }
            }
            finishedSuccessfully = true;
        }
        finally
        {
            printFinishTime( logger, start, finishedSuccessfully);
        }

        return result;
    }

    private boolean isRecoverable(int httpErrorCode)
    {
        return httpErrorCode == HttpStatus.SC_GATEWAY_TIMEOUT || httpErrorCode == HttpStatus.SC_SERVICE_UNAVAILABLE;
    }

    private void waitBeforeRetry(int seconds) {
        try
        {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        }
        catch (InterruptedException e)
        {
            logger.error( "Caught exception while waiting", e );
        }
    }

    private class Task
    {
        private final List<ProjectVersionRef> chunk;

        private final String endpointUrl;

        private Map<ProjectVersionRef, String> result = Collections.emptyMap();

        private int status = -1;

        private Exception exception;

        private String errorString;


        Task( List<ProjectVersionRef> chunk, String endpointUrl )
        {
            this.chunk = chunk;
            this.endpointUrl = endpointUrl;
        }

        void executeTranslate()
        {
            HttpResponse<List<MavenLookupResult>> r;

            try
            {
                MavenLookupRequest request = MavenLookupRequest
                                .builder()
                                .mode( mode )
                                .brewPullActive( brewPullActive )
                                .artifacts( GAVUtils.generateGAVs( chunk ) )
                                .build();


                r = Unirest.post( this.endpointUrl )
                           .header( "accept", "application/json" )
                           .header( "Content-Type", "application/json" )
                           .headers( restHeaders )
                           .connectTimeout(restConnectionTimeout * 1000)
                           .socketTimeout(restSocketTimeout * 1000)
                           .body( request )
                           .asObject( lookupType )
                           .ifSuccess( successResponse -> result = successResponse.getBody()
                                                                                  .stream()
                                                                                  .filter( f -> isNotBlank( f.getBestMatchVersion() ) )
                                                                                  .collect(
                                                                                                  Collectors.toMap(
                                                                                                  e -> ( (ExtendedMavenLookupResult) e ).getProjectVersionRef(),
                                                                                                  MavenLookupResult::getBestMatchVersion,
                                                                                                  // If there is a duplicate key, use the original.
                                                                                                  (o, n) -> {
                                                                                                      logger.warn( "Located duplicate key {}", o);
                                                                                                      return o;
                                                                                                  } )
                                                                                  )
                           )
                           .ifFailure( failedResponse -> {
                               if ( !failedResponse.getParsingError().isPresent() )
                               {
                                   logger.debug( "Parsing error but no message. Status text {}", failedResponse.getStatusText() );
                                   throw new ManipulationUncheckedException( failedResponse.getStatusText() );
                               }
                               else
                               {
                                   String originalBody = failedResponse.getParsingError().get().getOriginalBody();

                                   if ( originalBody.length() == 0 )
                                   {
                                       this.errorString = "No content to read.";
                                   }
                                   else if ( originalBody.startsWith( "<" ) )
                                   {
                                       // Read an HTML string.
                                       String stripped = originalBody.replaceAll( "<.*?>", "" ).replaceAll( "\n", " " ).trim();
                                       logger.debug( "Read HTML string '{}' rather than a JSON stream; stripping message to '{}'",
                                                     originalBody, stripped );
                                       this.errorString = stripped;
                                   }
                                   else if ( originalBody.startsWith( "{\"" ) )
                                   {
                                       this.errorString = failedResponse.mapError( ErrorMessage.class ).toString();

                                       logger.debug( "Read message string {}, processed to {} ", originalBody, errorString );
                                   }
                                   else if (originalBody.startsWith( "javax.validation.ValidationException: " )) {
                                       this.errorString = originalBody;
                                   }
                                   else
                                   {
                                       logger.error( "### HTTP comm failure: {}", failedResponse.getParsingError().get().getMessage() );
                                       throw new ManipulationUncheckedException( "Problem in HTTP communication with status code {} and message {}",
                                                                                 failedResponse.getStatus(), failedResponse.getStatusText() );
                                   }
                               }
                           } );

                status = r.getStatus();
            }
            catch ( ManipulationUncheckedException | UnirestException e )
            {
                exception = e;
                this.status = -1;
            }
        }

        public List<Task> split()
        {
            List<Task> res = new ArrayList<>( CHUNK_SPLIT_COUNT );
            if ( chunk.size() >= CHUNK_SPLIT_COUNT )
            {
                // To KISS, overflow the remainder into the last chunk
                int chunkSize = chunk.size() / CHUNK_SPLIT_COUNT;
                for ( int i = 0; i < ( CHUNK_SPLIT_COUNT - 1 ); i++ )
                {
                    res.add( new Task( chunk.subList( i * chunkSize, ( i + 1 ) * chunkSize ), endpointUrl ) );
                }
                // Last chunk may have different size
                res.add( new Task( chunk.subList( ( CHUNK_SPLIT_COUNT - 1 ) * chunkSize, chunk.size() ),
                                   endpointUrl ) );
            }
            else
            {
                for ( int i = 0 ; i < ( chunk.size() - initialRestMinSize ) + 1; i++ )
                {
                    res.add( new Task( chunk.subList( i * initialRestMinSize, ( i + 1 ) * initialRestMinSize ), endpointUrl ) );
                }
            }
            return res;
        }

        boolean canSplit()
        {
            return ( chunk.size() / initialRestMinSize ) > 0 && chunk.size() != 1;
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

        String getErrorMessage()
        {
            return (exception != null ? exception.getMessage() + ' ' : "" ) + ( errorString != null ? errorString : "" );
        }

        int getChunkSize()
        {
            return chunk.size();
        }
    }

    private static void printFinishTime ( Logger logger, long start, boolean finished )
    {
        long finish = System.nanoTime();
        long minutes = TimeUnit.NANOSECONDS.toMinutes( finish - start );
        long seconds = TimeUnit.NANOSECONDS.toSeconds( finish - start ) - ( minutes * 60 );
        logger.info ( "REST client finished {}... (took {} min, {} sec, {} millisec)",
                      ( finished ? "successfully" : "with failures"), minutes, seconds,
                      (TimeUnit.NANOSECONDS.toMillis( finish - start ) - ( minutes * 60 * 1000 ) - ( seconds * 1000) ));
    }
}
