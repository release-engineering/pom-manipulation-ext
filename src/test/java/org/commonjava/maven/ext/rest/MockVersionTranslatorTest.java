package org.commonjava.maven.ext.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author vdedik@redhat.com
 */
public class MockVersionTranslatorTest {
    private VersionTranslator versionTranslator;

    @Before
    public void startUp() {
        this.versionTranslator = new MockVersionTranslator();
    }

    @Test
    public void testTranslateVersionsSuccess() {
        ProjectVersionRef project = new ProjectVersionRef("org.overlord.rtgov", "parent", "2.0.2");
        List<ProjectVersionRef> deps = new ArrayList<ProjectVersionRef>() {{
            // Not important
        }};

        List<ProjectVersionRef> actualResult = versionTranslator.translateVersions(project, deps);
        List<ProjectVersionRef> expectedResult = new ArrayList<ProjectVersionRef>() {{
            add(new ProjectVersionRef("org.overlord.rtgov", "parent", "2.0.2.redhat-1"));
            add(new ProjectVersionRef("org.jboss.soa.bpel", "riftsaw-bpel-api", "3.1.0.Final-redhat-1"));
            add(new ProjectVersionRef("org.overlord.rtgov.common", "rtgov-elasticsearch", "2.0.2.redhat-1"));
        }};

        assertThat(actualResult, is(expectedResult));
    }
}
