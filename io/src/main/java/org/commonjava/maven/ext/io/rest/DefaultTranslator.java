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
package org.commonjava.maven.ext.io.rest;

import kong.unirest.GenericType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.apache.commons.codec.binary.Base32;
import org.apache.http.HttpStatus;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.json.ErrorMessage;
import org.commonjava.maven.ext.common.json.ExtendedLookupReport;
import org.commonjava.maven.ext.common.util.GAVUtils;
import org.commonjava.maven.ext.common.util.JSONUtils.InternalObjectMapper;
import org.commonjava.maven.ext.common.util.ListUtils;
import org.commonjava.maven.ext.io.rest.exception.RestException;
import org.jboss.da.reports.model.request.LookupGAVsRequest;
import org.jboss.da.reports.model.response.LookupReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * @author ncross@redhat.com
 * @author vdedik@redhat.com
 * @author jsenko@redhat.com
 */
public class DefaultTranslator
    implements Translator
{
    private static final GenericType<List<LookupReport>> lookupType = new GenericType<List<LookupReport>>()
    {
    };
    private static final GenericType<List<ProjectVersionRef>> pvrTyoe = new GenericType<List<ProjectVersionRef>>()
    {
    };

    private static final String REPORTS_LOOKUP_GAVS = "reports/lookup/gavs";

    private static final String LISTING_BLACKLIST_GA = "listings/blacklist/ga";

    @SuppressWarnings("WeakerAccess") // Public API.
    protected static final Random RANDOM = new Random();

    @SuppressWarnings("WeakerAccess") // Public API.
    protected static final Base32 CODEC = new Base32();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String endpointUrl;

    private final int initialRestMaxSize;

    private final int initialRestMinSize;

    private final String repositoryGroup;

    private final String incrementalSerialSuffix;

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

    // Allow test to override this.
    @SuppressWarnings( "FieldCanBeLocal" )
    private int retryDuration = 30;

    /**
     * @param endpointUrl is the URL to talk to.
     * @param restMaxSize initial (maximum) size of the rest call; if zero will send everything.
     * @param restMinSize minimum size for the call
     * @param repositoryGroup the group to pass to the endpoint.
     * @param incrementalSerialSuffix the suffix to pass to the endpoint.
     */
    public DefaultTranslator( String endpointUrl, int restMaxSize, int restMinSize, String repositoryGroup, String incrementalSerialSuffix )
    {
        this.repositoryGroup = repositoryGroup;
        this.incrementalSerialSuffix = incrementalSerialSuffix;
        this.endpointUrl = endpointUrl + ( isNotBlank( endpointUrl ) ? endpointUrl.endsWith( "/" ) ? "" : "/" : "");
        this.initialRestMaxSize = restMaxSize;
        this.initialRestMinSize = restMinSize;
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

        queue.add(new Task( projects, endpointUrl + REPORTS_LOOKUP_GAVS));
    }

    private void userDefinedPartition(List<ProjectVersionRef> projects, Queue<Task> queue) {
        logger.info("Using user defined partition strategy");

        // Presplit
        final List<List<ProjectVersionRef>> partition = ListUtils.partition( projects, initialRestMaxSize );

        for ( List<ProjectVersionRef> p : partition )
        {
            queue.add( new Task( p, endpointUrl + REPORTS_LOOKUP_GAVS ) );
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
            queue.add( new Task( p, endpointUrl + REPORTS_LOOKUP_GAVS ) );
        }
    }

    @Override
    public List<ProjectVersionRef> findBlacklisted( ProjectRef ga )
    {
        final String blacklistEndpointUrl = endpointUrl + LISTING_BLACKLIST_GA;
        final AtomicReference<List<ProjectVersionRef>> result = new AtomicReference<>();
        final String[] errorString = new String[1];
        final HttpResponse<List<ProjectVersionRef>> r;

        logger.trace( "Called findBlacklisted to {} with {}", blacklistEndpointUrl, ga );

        try
        {
            r = Unirest.get( blacklistEndpointUrl )
                       .header( "accept", "application/json" )
                       .header( "Content-Type", "application/json" )
                       .header( "Log-Context", getHeaderContext() )
                       .queryString( "groupid", ga.getGroupId() )
                       .queryString( "artifactid", ga.getArtifactId() )
                       .asObject( pvrTyoe )
                       .ifSuccess( successResponse -> result.set( successResponse.getBody() ) )
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
                                   errorString[0] = "No content to read.";
                               }
                               else if ( originalBody.startsWith( "<" ) )
                               {
                                   // Read an HTML string.
                                   String stripped = originalBody.replaceAll( "<.*?>", "" ).replaceAll( "\n", " " ).trim();
                                   logger.debug( "Read HTML string '{}' rather than a JSON stream; stripping message to '{}'",
                                                 originalBody, stripped );
                                   errorString[0] = stripped;
                               }
                               else if ( originalBody.startsWith( "{\"" ) )
                               {
                                   errorString[0] = failedResponse.mapError( ErrorMessage.class ).toString();

                                   logger.debug( "Read message string {}, processed to {} ", originalBody, errorString );
                               }
                               else
                               {
                                   throw new ManipulationUncheckedException( "Unknown error",
                                                                             failedResponse.getParsingError().get() );
                               }
                           }
                       } );

            if ( !r.isSuccess() )
            {
                throw new RestException( String.format( "Failed to establish blacklist calling %s with error %s", this.endpointUrl, errorString[0] ) );
            }
        }
        catch ( ManipulationUncheckedException | UnirestException e )
        {
            throw new RestException( "Unable to contact DA", e );
        }

        return result.get();
    }



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
    public Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> p )
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

                        if ( task.getStatus() > 0 )
                        {
                            throw new RestException( "Received response status " + task.getStatus() + " with message: "
                                                                     + task.getErrorMessage() );
                        }
                        else
                        {
                            throw new RestException( "Received response status " + task.getStatus() + " with message " + task.getErrorMessage() );
                        }
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

    /**
     * Returns the current log header. Protected so it can be overridden.
     * @return a String header
     */
    @SuppressWarnings("WeakerAccess") // Public API.
    protected String getHeaderContext ()
    {
        String headerContext;

        if ( isNotEmpty( org.slf4j.MDC.get( "LOG-CONTEXT" ) ) )
        {
            headerContext = org.slf4j.MDC.get( "LOG-CONTEXT" );
        }
        else
        {
            // If we have no MDC PME has been used as the entry point. Dummy one up for DA.
            byte[] randomBytes = new byte[20];
            RANDOM.nextBytes( randomBytes );
            headerContext = "pme-" + CODEC.encodeAsString( randomBytes );
        }

        return headerContext;
    }


    private class Task
    {
        private List<ProjectVersionRef> chunk;

        private Map<ProjectVersionRef, String> result = Collections.emptyMap();

        private int status = -1;

        private Exception exception;

        private String errorString;

        private String endpointUrl;

        Task( List<ProjectVersionRef> chunk, String endpointUrl )
        {
            this.chunk = chunk;
            this.endpointUrl = endpointUrl;
        }

        void executeTranslate()
        {
            HttpResponse<List<LookupReport>> r;

            try
            {
                LookupGAVsRequest request =
                                new LookupGAVsRequest( Collections.emptySet(), Collections.emptySet(), repositoryGroup,
                                                       incrementalSerialSuffix, GAVUtils.generateGAVs( chunk ) );

                r = Unirest.post( this.endpointUrl )
                           .header( "accept", "application/json" )
                           .header( "Content-Type", "application/json" )
                           .header( "Log-Context", getHeaderContext() )
                           .body( request )
                           .asObject( lookupType )
                           .ifSuccess( successResponse -> result = successResponse.getBody()
                                                                                  .stream()
                                                                                  .filter( f -> isNotBlank( f.getBestMatchVersion() ) )
                                                                                  .collect( Collectors.toMap(
                                                                                                  e -> ( (ExtendedLookupReport) e ).getProjectVersionRef(),
                                                                                                  LookupReport::getBestMatchVersion,
                                                                                                  // If there is a duplicate key, use the original.
                                                                                                  (o, n) -> {
                                                                                                      logger.warn( "Located duplicate key {}", o);
                                                                                                      return o;
                                                                                                  } ) ) )
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
                                   else
                                   {
                                       throw new ManipulationUncheckedException( "Unknown error", failedResponse.getParsingError().get() );
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
