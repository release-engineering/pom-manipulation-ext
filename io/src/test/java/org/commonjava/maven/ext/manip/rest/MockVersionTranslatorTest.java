package org.commonjava.maven.ext.manip.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vdedik@redhat.com
 */
public class MockVersionTranslatorTest {
    private static MockVersionTranslator versionTranslator;

    @BeforeClass
    public static void startUp() {
        versionTranslator = new MockVersionTranslator();
    }

    @AfterClass
    public static void tearDown() {
        versionTranslator.shutdownMockServer();
    }

    @Test
    public void testTranslateVersionsSuccess() {
        ProjectVersionRef project = new ProjectVersionRef("org.overlord.rtgov", "parent", "2.0.2");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            add(new ProjectVersionRef("org.jboss.soa.bpel", "riftsaw-bpel-api", "3.1.0.Final"));
        }};

        List<ProjectVersionRef> actualResult = versionTranslator.translateVersions(project, deps);
        List<ProjectVersionRef> expectedResult = new ArrayList<ProjectVersionRef>() {{
            add(new ProjectVersionRef("org.overlord.rtgov", "parent", "2.0.2-redhat-1"));
            add(new ProjectVersionRef("org.jboss.soa.bpel", "riftsaw-bpel-api", "3.1.0.Final-redhat-1"));
        }};

        assertThat(actualResult, is(expectedResult));
    }
}
