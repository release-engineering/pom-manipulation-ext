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
package org.commonjava.maven.ext.manip.util;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.fixture.TestUtils;
import org.commonjava.maven.ext.manip.impl.RESTManipulator;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ResolveArtifactTest
{

    @Test
    public void testResolveArtifacts() throws Exception
    {
        final ManipulationSession session = new ManipulationSession();

        // Locate the PME project pom file.
        final File projectroot = new File (TestUtils.resolveFileResource( "properties/", "" )
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );

        Set<ArtifactRef> artifacts = RESTManipulator.establishAllDependencies( session, projects, null );

        // NB If this test fails then check if PME deps/plugins have changed...
        assertTrue ( artifacts.size() == 59 );
    }
}
