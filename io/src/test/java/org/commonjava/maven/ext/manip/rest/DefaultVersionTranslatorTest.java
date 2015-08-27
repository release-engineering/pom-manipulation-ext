package org.commonjava.maven.ext.manip.rest;

import com.mashape.unirest.http.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.rule.MockServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.*;

/**
 * @author vdedik@redhat.com
 */
public class DefaultVersionTranslatorTest
{
    private static List<ProjectVersionRef> aLotOfGavs;

    private DefaultVersionTranslator versionTranslator;

    @ClassRule
    public static MockServer mockServer = new MockServer();

    @BeforeClass
    public static void startUp()
    {
        aLotOfGavs = new ArrayList<ProjectVersionRef>();
        String longJsonFile = readFileFromClasspath( "example-response-performance-test.json" );
        JSONArray jsonGavs = new JSONArray( longJsonFile );
        for ( Integer i = 0; i < jsonGavs.length(); i++ )
        {
            JSONObject jsonGav = jsonGavs.getJSONObject( i );

            ProjectVersionRef gav =
                new ProjectVersionRef( jsonGav.getString( "groupId" ), jsonGav.getString( "artifactId" ),
                                       jsonGav.getString( "version" ) );
            aLotOfGavs.add( gav );
        }
    }

    @Before
    public void before()
    {
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
    public void testTranslateVersionsSuccess()
    {
        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
                add( new ProjectVersionRef( "com.example", "example", "1.0" ) );
                add( new ProjectVersionRef( "com.example", "example-dep", "2.0" ) );
                add( new ProjectVersionRef( "org.commonjava", "example", "1.0" ) );
                add( new ProjectVersionRef( "org.commonjava", "example", "1.1" ) );
            }};

        Map<ProjectVersionRef, String> actualResult = versionTranslator.translateVersions( gavs );
        Map<ProjectVersionRef, String> expectedResult = new HashMap<ProjectVersionRef, String>()
        {{
                put( new ProjectVersionRef( "com.example", "example", "1.0" ), "1.0-redhat-1" );
                put( new ProjectVersionRef( "com.example", "example-dep", "2.0" ), "2.0-redhat-1" );
                put( new ProjectVersionRef( "org.commonjava", "example", "1.0" ), "1.0-redhat-1" );
                put( new ProjectVersionRef( "org.commonjava", "example", "1.1" ), "1.1-redhat-1" );
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
                add( new ProjectVersionRef( "com.example", "example", "1.0" ) );
            }};

        try
        {
            versionTranslator.translateVersions( gavs );
            fail( "Failed to throw RestException when server failed to respond." );
        }
        catch ( RestException ex )
        {
            // Pass
        }
        catch ( Exception ex )
        {
            fail( String.format( "Expected exception is RestException, instead %s thrown.",
                                 ex.getClass().getSimpleName() ) );
        }
    }

    @Test( timeout = 300 )
    public void testTranslateVersionsPerformance()
    {
        versionTranslator.translateVersions( aLotOfGavs );
    }

    private static String readFileFromClasspath( String filename )
    {
        StringBuilder fileContents = new StringBuilder();
        Scanner scanner = new Scanner( DefaultVersionTranslatorTest.class.getResourceAsStream( filename ) );
        String lineSeparator = System.getProperty( "line.separator" );

        try
        {
            while ( scanner.hasNextLine() )
            {
                fileContents.append( scanner.nextLine() + lineSeparator );
            }
            return fileContents.toString();
        }
        finally
        {
            scanner.close();
        }
    }
}
