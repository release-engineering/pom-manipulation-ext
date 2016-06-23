/**
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.commonjava.maven.ext.manip.rest;

import com.mashape.unirest.http.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.handler.SpyFailJettyHandler;
import org.commonjava.maven.ext.manip.rest.rule.MockServer;
import org.junit.*;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.maven.ext.manip.rest.DefaultVersionTranslatorTest.loadALotOfGAVs;
import static org.junit.Assert.*;

/**
 * @author Jakub Senko <jsenko@redhat.com>
 */
public class DefaultVersionTranslatorSplitTest
{

    private static List<ProjectVersionRef> aLotOfGavs;

    private DefaultVersionTranslator versionTranslator;

    @Rule public TestName testName = new TestName();

    private static SpyFailJettyHandler handler = new SpyFailJettyHandler();

    @ClassRule public static MockServer mockServer = new MockServer( handler );

    private static final Logger LOG = LoggerFactory.getLogger( DefaultVersionTranslatorSplitTest.class );

    @BeforeClass public static void startUp()
                    throws IOException
    {
        aLotOfGavs = loadALotOfGAVs();
        assertTrue( aLotOfGavs.size() >= 37 );
    }

    @Before public void before()
    {

        LOG.info( "Executing test " + testName.getMethodName() );

        this.versionTranslator = new DefaultVersionTranslator( mockServer.getUrl() );
    }

    @Test public void testConnection()
    {
        try
        {
            Unirest.post( this.versionTranslator.getEndpointUrl() ).asString();
        }
        catch ( Exception e )
        {
            fail( "Failed to connect to server, exception message: " + e.getMessage() );
        }
    }

    @Test public void testTranslateVersionsCorrectSplit()
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

    @Test public void testTranslateVersionsCorrectSplit2()
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

}
