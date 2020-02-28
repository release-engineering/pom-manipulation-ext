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
package org.commonjava.maven.ext.core.impl;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.core.ManipulationManager;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.io.PomIO;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 22/08/2019
 */
public class InitialGroovyManipulatorTest {

    @Test
    public void shouldRemoveProjectInGroovyScript() throws Exception {
        final File groovy = TestUtils.resolveFileResource( "groovy-project-removal", "/manipulation.groovy" );
        final File projectroot = TestUtils.resolveFileResource( "groovy-project-removal", "/pom.xml" );

        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning( PlexusConstants.SCANNING_ON );
        config.setComponentVisibility( PlexusConstants.GLOBAL_VISIBILITY );
        config.setName( "PME-CLI" );
        PlexusContainer container = new DefaultPlexusContainer( config);

        PomIO pomIO = container.lookup( PomIO.class );
        List<Project> projects = pomIO.parseProject( projectroot );

        assertThat( projects.size(), equalTo(3) );

        Properties userProperties = new Properties(  );
        userProperties.setProperty( "versionIncrementalSuffix", "rebuild" );

        userProperties.setProperty( "groovyScripts", "file://" + groovy.getAbsolutePath() );

        ManipulationManager manipulationManager = container.lookup( ManipulationManager.class );
        ManipulationSession session = container.lookup( ManipulationSession.class );

        MavenExecutionRequest req = new DefaultMavenExecutionRequest().setSystemProperties( System.getProperties() )
              .setUserProperties( userProperties )
              .setRemoteRepositories( Collections.emptyList() );
        req.setPom( projectroot );
        MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );
        session.setMavenSession( mavenSession );
        manipulationManager.init( session );

        manipulationManager.scanAndApply( session );

        // re-read the projects:
        projects = pomIO.parseProject( projectroot );
        assertThat( projects.size(), equalTo(3) );

        assertThat( projectForArtifactId(projects, "groovy-project-removal").getVersion(), containsString("rebuild") );
        assertThat( projectForArtifactId(projects, "groovy-project-removal-moduleA").getVersion(), containsString("rebuild") );
        // moduleB was removed from projects by the groovy script and therefore should not be reversioned:
        assertThat( projectForArtifactId(projects, "groovy-project-removal-moduleB").getVersion(), equalTo("1.0.0") );
    }

    private Project projectForArtifactId(List<Project> projects, String artifactId) {
        Optional<Project> maybeProject = projects.stream()
              .filter(p -> p.getArtifactId().equals(artifactId))
              .findAny();
        if (!maybeProject.isPresent()) {
            fail("Unable to find project " + artifactId + " in the groovy-project-removal project");
        }
        return maybeProject.get();
    }
}
