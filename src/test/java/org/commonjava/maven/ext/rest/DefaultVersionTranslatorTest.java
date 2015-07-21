package org.commonjava.maven.ext.rest;

import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverRule;
import static com.github.restdriver.clientdriver.RestClientDriver.*;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.rest.exception.ClientException;
import org.commonjava.maven.ext.rest.exception.RestException;
import org.commonjava.maven.ext.rest.exception.ServerException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author vdedik@redhat.com
 */
public class DefaultVersionTranslatorTest {
    private VersionTranslator versionTranslator;

    @Rule
    public ClientDriverRule driver = new ClientDriverRule();

    @Before
    public void startUp() {
        this.versionTranslator = new DefaultVersionTranslator(driver.getBaseUrl());
    }

    @Test
    public void testTranslateVersionsSuccess() {
        Pattern projectMatcher = Utils.getProjectMatcher("com.example:example:1.0");
        driver.addExpectation(onRequestTo("/").withMethod(Method.POST).withBody(projectMatcher, "application/json"),
                giveResponse("{'project':'com.example:example:1.0-redhat-1', " +
                                "'dependencies': ['com.example:example-dep:1.0-redhat-1']}",
                        "application/json"));

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
    public void testTranslateVersionsFail5xx() {
        driver.addExpectation(onRequestTo("/").withMethod(Method.POST),
                giveEmptyResponse().withStatus(500));

        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            // Nothing necessary here
        }};

        try {
            versionTranslator.translateVersions(project, deps);
            fail("Failed to throw ServerException when server responded with status code 500.");
        } catch (ServerException ex) {
            // Pass
        } catch (Exception ex) {
            fail(String.format("Expected exception is ServerException, instead %s thrown.",
                    ex.getClass().getSimpleName()));
        }
    }

    @Test
    public void testTranslateVersionsFail4xx() {
        driver.addExpectation(onRequestTo("/").withMethod(Method.POST),
                giveEmptyResponse().withStatus(400));

        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            // Nothing necessary here
        }};

        try {
            versionTranslator.translateVersions(project, deps);
            fail("Failed to throw ClientException when server responded with status code 400.");
        } catch (ClientException ex) {
            // Pass
        } catch (Exception ex) {
            fail(String.format("Expected exception is ClientException, instead %s thrown.",
                    ex.getClass().getSimpleName()));
        }
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

    @Test(timeout=200)
    public void testTranslateVersionsPerformance() {
        driver.addExpectation(onRequestTo("/").withMethod(Method.POST),
                giveResponse(Utils.readFileFromClasspath(
                        "example-response-performance-test.json"), "application/json"));

        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            // Nothing necessary here
        }};

        versionTranslator.translateVersions(project, deps);
    }
}
