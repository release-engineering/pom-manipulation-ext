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

import com.mashape.unirest.http.Unirest;
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
import java.util.*;

import static org.commonjava.maven.ext.io.rest.Translator.RestProtocol.CURRENT;
import static org.commonjava.maven.ext.io.rest.VersionTranslatorTest.loadALotOfGAVs;
import static org.junit.Assert.*;

/**
 * @author Jakub Senko <jsenko@redhat.com>
 */
@RunWith( Parameterized.class)
public class HandleServiceUnavailableTest
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

    private static final Logger LOG = LoggerFactory.getLogger( HandleServiceUnavailableTest.class );

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

        handler.setStatusCode( HttpServletResponse.SC_SERVICE_UNAVAILABLE );
        versionTranslator = new DefaultTranslator( mockServer.getUrl(), protocol, 0, Translator.CHUNK_SPLIT_COUNT,
                                                   "", "" );
    }

    public HandleServiceUnavailableTest(Translator.RestProtocol protocol )
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

    /**
     * Ensures that the version translation does not fail upon receiving 503
     * and follow the same behavior as that of a 504.
     */
    @Test
    public void testTranslateVersionsCorrectSplit()
    {
        List<ProjectVersionRef> data = aLotOfGavs.subList( 0, 37 );
        handler.getRequestData().clear();
        try
        {
            // Decrease the wait time so that the test does not take too long
            versionTranslator.setRetryDuration(5);
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
}
