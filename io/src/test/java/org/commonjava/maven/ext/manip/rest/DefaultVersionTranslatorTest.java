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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.manip.rest.rule.MockServer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author vdedik@redhat.com
 */
@FixMethodOrder( MethodSorters.NAME_ASCENDING)
public class DefaultVersionTranslatorTest
{
    private static List<ProjectVersionRef> aLotOfGavs;

    private DefaultVersionTranslator versionTranslator;

    @Rule
    public TestName testName = new TestName();

    @ClassRule
    public static MockServer mockServer = new MockServer( new AddSuffixJettyHandler() );

    @BeforeClass
    public static void startUp()
                    throws IOException
    {
        aLotOfGavs = loadALotOfGAVs();
    }

    @Before
    public void before()
    {
        LoggerFactory.getLogger( DefaultVersionTranslator.class ).info( "Executing test " + testName.getMethodName() );

        this.versionTranslator = new DefaultVersionTranslator( mockServer.getUrl() );
    }

    @Test
    public void testConnection()
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

    @Test
    public void testTranslateVersions()
    {
        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
            add( new SimpleProjectVersionRef( "com.example", "example", "1.0" ) );
            add( new SimpleProjectVersionRef( "com.example", "example-dep", "2.0" ) );
            add( new SimpleProjectVersionRef( "org.commonjava", "example", "1.0" ) );
            add( new SimpleProjectVersionRef( "org.commonjava", "example", "1.1" ) );
        }};

        Map<ProjectVersionRef, String> actualResult = versionTranslator.translateVersions( gavs );
        Map<ProjectVersionRef, String> expectedResult = new HashMap<ProjectVersionRef, String>()
        {{
            put( new SimpleProjectVersionRef( "com.example", "example", "1.0" ), "1.0-redhat-1" );
            put( new SimpleProjectVersionRef( "com.example", "example-dep", "2.0" ), "2.0-redhat-1" );
            put( new SimpleProjectVersionRef( "org.commonjava", "example", "1.0" ), "1.0-redhat-1" );
            put( new SimpleProjectVersionRef( "org.commonjava", "example", "1.1" ), "1.1-redhat-1" );
        }};

        assertThat( actualResult, is( expectedResult ) );
    }

    @Test
    public void testTranslateVersionsFailNoResponse()
    {
        // Some url that doesn't exist used here
        VersionTranslator versionTranslator = new DefaultVersionTranslator( "http://127.0.0.2" );

        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
            add( new SimpleProjectVersionRef( "com.example", "example", "1.0" ) );
        }};

        try
        {
            versionTranslator.translateVersions( gavs );
            fail( "Failed to throw RestException when server failed to respond." );
        }
        catch ( RestException ex )
        {
            System.out.println( "Caught ex" + ex );
            // Pass
        }
        catch ( Exception ex )
        {
            fail( String.format( "Expected exception is RestException, instead %s thrown.",
                                 ex.getClass().getSimpleName() ) );
        }
    }

    @Test( timeout = 500 )
    public void testTranslateVersionsPerformance()
    {
        // Disable logging for this test as impacts timing.
        ( (Logger) LoggerFactory.getLogger( Logger.ROOT_LOGGER_NAME ) ).setLevel( Level.WARN );

        versionTranslator.translateVersions( aLotOfGavs );
    }


    private static String readFileFromClasspath( String filename )
    {
        StringBuilder fileContents = new StringBuilder();
        String lineSeparator = System.getProperty( "line.separator" );

        try (Scanner scanner = new Scanner( DefaultVersionTranslatorTest.class.getResourceAsStream( filename ) ))
        {
            while ( scanner.hasNextLine() )
            {
                fileContents.append( scanner.nextLine() ).append( lineSeparator );
            }
            return fileContents.toString();
        }
    }


    public static List<ProjectVersionRef> loadALotOfGAVs() throws IOException {
        List<ProjectVersionRef> result = new ArrayList<>();
        String longJsonFile = readFileFromClasspath( "example-response-performance-test.json" );

        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, String>> gavs = objectMapper
                .readValue( longJsonFile, new TypeReference<List<Map<String, String>>>() {} );

        for ( Map<String, String> gav : gavs )
        {
            ProjectVersionRef project = new SimpleProjectVersionRef( gav.get( "groupId" ), gav.get( "artifactId" ), gav.get( "version" ) );
            result.add( project );
        }
        return result;
    }
}
