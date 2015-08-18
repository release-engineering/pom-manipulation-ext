package org.commonjava.maven.ext.rest;


import com.mashape.unirest.http.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.rest.exception.RestException;
import org.commonjava.maven.ext.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.server.HttpServer;
import org.commonjava.maven.ext.server.JettyHttpServer;
import org.eclipse.jetty.server.Handler;
import org.json.JSONArray;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vdedik@redhat.com
 */
public class DefaultVersionTranslatorTest {
    private DefaultVersionTranslator versionTranslator;
    private static HttpServer httpServer;
    private static List<ProjectVersionRef> aLotOfDeps;

    @BeforeClass
    public static void startUp() {
        Handler handler = new AddSuffixJettyHandler();
        httpServer = new JettyHttpServer(handler);

        aLotOfDeps = new ArrayList<ProjectVersionRef>();
        String longJsonFile = Utils.readFileFromClasspath("example-response-performance-test.json");
        JSONArray jsonDeps = new JSONArray(longJsonFile);
        for (Integer i = 0; i < jsonDeps.length(); i++) {
            aLotOfDeps.add(Utils.fromString(jsonDeps.getString(i)));
        }
    }

    @AfterClass
    public static void tearDown() {
        httpServer.shutdown();
    }

    @Before
    public void initTest() {
        this.versionTranslator = new DefaultVersionTranslator("http://127.0.0.1:" + httpServer.getPort());
    }

    @Test
    public void testConnection() {
        try {
            Unirest.post(this.versionTranslator.getEndpointUrl()).asString();
        } catch (Exception e) {
            fail("Failed to connect to server, exception message: " + e.getMessage());
        }
    }

    @Test
    public void testTranslateVersionsSuccess() {
        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            add(new ProjectVersionRef("com.example", "example-dep", "1.0"));
        }};

        List<ProjectVersionRef> actualResult = versionTranslator.translateVersions(project, deps);
        List<ProjectVersionRef> expectedResult = new ArrayList<ProjectVersionRef>() {{
            add(new ProjectVersionRef("com.example", "example", "1.0-redhat-1"));
            add(new ProjectVersionRef("com.example", "example-dep", "1.0-redhat-1"));
        }};

        assertThat(actualResult, is(expectedResult));
    }


    @Test
    public void testTranslateVersionsFailNoResponse() {
        // Some url that doesn't exist used here
        VersionTranslator versionTranslator = new DefaultVersionTranslator("http://127.0.0.2");

        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            // Nothing necessary here
        }};

        try {
            versionTranslator.translateVersions(project, deps);
            fail("Failed to throw RestException when server failed to respond.");
        } catch (RestException ex) {
            // Pass
        } catch (Exception ex) {
            fail(String.format("Expected exception is RestException, instead %s thrown.",
                    ex.getClass().getSimpleName()));
        }
    }

    @Test(timeout=300)
    public void testTranslateVersionsPerformance() {
        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        versionTranslator.translateVersions(project, aLotOfDeps);
    }
}
