package org.commonjava.maven.ext.rest;

import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriverRule;
import static com.github.restdriver.clientdriver.RestClientDriver.*;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public class VersionTranslatorTest {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule();

    @Before
    public void startUp() {
        driver.addExpectation(onRequestTo("/").withMethod(Method.POST),
                giveResponse("{'project':'com.example:example:1.0-redhat-1', 'dependencies': []}",
                        "application/json"));
    }

    @Test
    public void testTranslateVersionsSuccess() {
        VersionTranslator versionTranslator = new VersionTranslator(driver.getBaseUrl());

        ProjectVersionRef project = new ProjectVersionRef("com.example", "example", "1.0");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>();

        List<ProjectVersionRef> actualResult = versionTranslator.translateVersions(project, deps);
        List<ProjectVersionRef> expectedResult = new ArrayList<ProjectVersionRef>() {{
            add(new ProjectVersionRef("com.example", "example", "1.0-redhat-1"));
        }};

        assertThat(actualResult, is(expectedResult));
    }
}
