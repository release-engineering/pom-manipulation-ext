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

import kong.unirest.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.io.rest.exception.RestException;
import org.commonjava.maven.ext.io.rest.handler.SpyFailJettyHandler;
import org.commonjava.maven.ext.io.rest.rule.MockServer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.maven.ext.io.rest.Translator.RestProtocol.CURRENT;
import static org.commonjava.maven.ext.io.rest.VersionTranslatorTest.loadALotOfGAVs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Jakub Senko <jsenko@redhat.com>
 */
@RunWith( Parameterized.class)
public class VersionTranslatorSplitTest
{
    private static List<ProjectVersionRef> aLotOfGavs;

    private DefaultTranslator versionTranslator;

    private Translator.RestProtocol protocol;

    @Parameterized.Parameters()
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][] { { CURRENT } } );
    }

    @Rule
    public TestName testName = new TestName();

    private static SpyFailJettyHandler handler = new SpyFailJettyHandler();

    @Rule
    public MockServer mockServer = new MockServer( handler );

    private static final Logger LOG = LoggerFactory.getLogger( VersionTranslatorSplitTest.class );

    @BeforeClass
    public static void startUp() throws IOException
    {
        aLotOfGavs = loadALotOfGAVs();
        assertTrue( aLotOfGavs.size() >= 37 );
    }

    @Before
    public void before()
    {
        LOG.info( "Executing test " + testName.getMethodName() );

        handler.setStatusCode( HttpServletResponse.SC_GATEWAY_TIMEOUT );
        versionTranslator = new DefaultTranslator( mockServer.getUrl(), 0, Translator.CHUNK_SPLIT_COUNT,
                                                   "", "" );
    }

    public VersionTranslatorSplitTest( Translator.RestProtocol protocol )
    {
        this.protocol = protocol;
    }

    @Test
    public void testConnection()
    {
        try
        {
            Unirest.post( mockServer.getUrl() ).asString();
        }
        catch ( Exception e )
        {
            fail( "Failed to connect to server, exception message: " + e.getMessage() );
        }
    }

    @Test
    public void testTranslateVersionsCorrectSplit()
    {
        List<ProjectVersionRef> data = aLotOfGavs.subList( 0, 37 );
        handler.getRequestData().clear();
        try
        {
            versionTranslator.translateVersions( data );
            fail();
        }
        catch ( RestException ex )
        {
            // ok
        }
        List<List<Map<String, Object>>> requestData = handler.getRequestData();
        // split 37 -> 9, 9, 9, 10
        // split 9 -> 2, 2, 2, 3 (x3)
        // split 10 -> 2, 2, 2, 4
        // split 4 -> 1, 1, 1, 1
        // 37, 9, 9, 9, 10, 2, 2, 2, 3, ..., 2, 2, 2, 4, 1, 1, 1, 1 -> total 21 chunks
        // However, split fails after the 6th attempt
        LOG.debug( requestData.toString() );
        assertEquals( 6, requestData.size() );
        assertEquals( 37, requestData.get( 0 ).size() );
        for ( int i = 1; i < 4; i++ )
            assertEquals( 9, requestData.get( i ).size() );
        assertEquals( 10, requestData.get( 4 ).size() );
        assertEquals( 2, requestData.get( 5 ).size() );

        Set<Map<String, Object>> original = new HashSet<>();
        original.addAll( requestData.get( 0 ) );

        Set<Map<String, Object>> chunks = new HashSet<>();
        for ( List<Map<String, Object>> e : requestData.subList( 1, 5 ) )
        {
            chunks.addAll( e );
        }
        assertEquals( original, chunks );
    }

    @Test
    public void testTranslateVersionsCorrectSplit2()
    {
        List<ProjectVersionRef> data = aLotOfGavs.subList( 0, 36 );
        handler.getRequestData().clear();
        try
        {
            versionTranslator.translateVersions( data );
            fail();
        }
        catch ( RestException ex )
        {
            // ok
        }
        List<List<Map<String, Object>>> requestData = handler.getRequestData();
        // split 36 -> 9, 9, 9, 9
        // split 9 -> 2, 2, 2, 3 (x4)
        // 36, 9, 9, 9, 9, 2, 2, 2, 3, ... -> total 21 chunks
        // Split fails after the 6th attempt
        LOG.debug( requestData.toString() );
        assertEquals( 6, requestData.size() );
        assertEquals( 36, requestData.get( 0 ).size() );
        for ( int i = 1; i < 5; i++ )
            assertEquals( 9, requestData.get( i ).size() );
        assertEquals( 2, requestData.get( 5 ).size() );

        Set<Map<String, Object>> original = new HashSet<>();
        original.addAll( requestData.get( 0 ) );

        Set<Map<String, Object>> chunks = new HashSet<>();
        for ( List<Map<String, Object>> e : requestData.subList( 1, 5 ) )
        {
            chunks.addAll( e );
        }
        assertEquals( original, chunks );
    }

    @Test
    public void testTranslateVersionsCorrectSplitMaxSize()
    {
        this.versionTranslator = new DefaultTranslator( mockServer.getUrl(), 10, Translator.CHUNK_SPLIT_COUNT,
                                                        "", "" );

        List<ProjectVersionRef> data = aLotOfGavs.subList( 0, 30 );
        handler.getRequestData().clear();
        try
        {
            versionTranslator.translateVersions( data );
            fail();
        }
        catch ( RestException ignored )
        {
        }
        List<List<Map<String, Object>>> requestData = handler.getRequestData();

        // split 10,10,10
        // split    10,10,2,2,2,4
        // split       10,2,2,2,4,2,2,2,4
        // split          2,2....
        // = 10 : 10 : 10 : 2
        LOG.debug( requestData.toString() );
        assertEquals( 4, requestData.size() );
        for ( int i = 0; i < 3; i++ )
        {
            assertEquals( 10, requestData.get( i ).size() );
        }
        assertEquals( 2, requestData.get( 3 ).size() );

        Set<Map<String, Object>> chunks = new HashSet<>();
        for ( List<Map<String, Object>> e : requestData.subList( 0, 3 ) )
        {
            chunks.addAll( e );
        }
        assertEquals( data.size(), chunks.size() );
    }

    @Test
    public void testTranslateVersionsNoSplitOnNon504()
    {
        List<ProjectVersionRef> data = aLotOfGavs.subList( 0, 36 );
        handler.getRequestData().clear();
        handler.setStatusCode( HttpServletResponse.SC_BAD_GATEWAY );
        try
        {
            versionTranslator.translateVersions( data );
            fail();
        }
        catch ( RestException ex )
        {
            // ok
        }
        List<List<Map<String, Object>>> requestData = handler.getRequestData();

        LOG.debug( requestData.toString() );

        // Due to this returning a non-504 it shouldn't do any splits so we should get size 1 containing 36 back
        assertEquals( 1, requestData.size() );
        assertEquals( 36, requestData.get( 0 ).size() );
    }

    @Test
    public void testTranslateVersionsCorrectSplitMaxSizeWithMin()
    {
        this.versionTranslator = new DefaultTranslator( mockServer.getUrl(), 10, 1, "",
                                                        "" );

        List<ProjectVersionRef> data = aLotOfGavs.subList( 0, 30 );
        handler.getRequestData().clear();
        try
        {
            versionTranslator.translateVersions( data );
            fail();
        }
        catch ( RestException ignored )
        {
        }
        List<List<Map<String, Object>>> requestData = handler.getRequestData();

        // split 30 ->
        //        10,10,10
        // split    10,10,2,2,2,4
        // split       10,2,2,2,4,2,2,2,4
        // split          2,2,2,4,2,2,2,4
        // split            2,2,4,2,2,2,4,2,2,2,4,1
        // split              2,4,2,2,2,4,2,2,2,4,1...
        // split                4,2,2,2,4,2,2,2,4,1...
        // split                  2,2,2,4,2,2,2,4,1...
        // split                    2,2,4,2,2,2,4,1...
        // split                      2,4,2,2,2,4,1...
        // split                        4,2,2,2,4,1...
        // split                          2,2,2,4,1...
        // split                            2,2,4,1...
        // split                              2,4,1...
        // split                                4,1...
        // split                                   1...
        // Count of 16 (all outer edges )
        LOG.debug( requestData.toString() );
        assertEquals( 16, requestData.size() );
        for ( int i = 0; i < 3; i++ )
        {
            assertEquals( 10, requestData.get( i ).size() );
        }
        assertEquals( 1, requestData.get( 15 ).size() );
    }
}
