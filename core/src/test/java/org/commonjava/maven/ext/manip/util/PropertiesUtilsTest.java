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
package org.commonjava.maven.ext.manip.util;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.fixture.TestUtils;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.CommonState;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.commonjava.maven.ext.manip.util.PropertiesUtils.updateProperties;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertiesUtilsTest
{
    private static final String RESOURCE_BASE = "properties/";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testCheckStrictValue() throws Exception
    {
        ManipulationSession session = createUpdateSession();
        assertFalse( PropertiesUtils.checkStrictValue( session, null, "1.0" ) );
        assertFalse( PropertiesUtils.checkStrictValue( session, "1.0", null ) );
        assertFalse ( PropertiesUtils.checkStrictValue( session, "1.0.0.Final", "1.0.0.redhat-1" ) );
        assertTrue ( PropertiesUtils.checkStrictValue( session, "1.0.0", "1.0.0.redhat-1" ) );
        assertTrue ( PropertiesUtils.checkStrictValue( session, "1.0.0-SNAPSHOT", "1.0.0.redhat-1" ) );
        assertFalse ( PropertiesUtils.checkStrictValue( session, "1.0.Final-SNAPSHOT", "1.0.0.redhat-1" ) );
        assertTrue ( PropertiesUtils.checkStrictValue( session, "1.0-SNAPSHOT", "1.0.0.redhat-1" ) );
    }

    @Test
    public void testCacheProperty() throws Exception
    {
        Map<Project,Map<String,String>> propertyMap = new HashMap<>();
        CommonState state = new CommonState( new Properties(  ) );
        Project project = getProject();

        assertFalse( PropertiesUtils.cacheProperty( project, state, propertyMap, "${foobar}${foobar2}", null, null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( project, state, propertyMap, "suffix.${foobar}", null, null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( project, state, propertyMap, null, "2.0", null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( project, state, propertyMap, "1.0", "2.0", null, false ) );
        assertTrue( PropertiesUtils.cacheProperty( project, state, propertyMap, "${version.org.jboss}", "2.0", null, false ) );
        assertFalse ( PropertiesUtils.cacheProperty( project, state, propertyMap, "${project.version}", "2.0", null, false ) );

        // DependencyManipulator does dependency.getVersion(). This could return e.g. ${version.scala} which can
        // refer to <version.scala>${version.scala.major}.7</version.scala>. If we are attempting to change version.scala
        // to e.g. 2.11.7.redhat-1 then in this case we need to ignore the version.scala.major property and append the .redhat-1.

        // If the property is ${...}.foobar then we only want to append suffix to foobar to change the version
        // However we don't need to change the value of the property. If the property is foobar.${....} then
        // we want to append suffix to the property ... but we need to handle that part of the property is hardcoded.

        assertFalse( PropertiesUtils.cacheProperty( project, state, propertyMap, "${version.scala}.7", "2.0", null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( project, state, propertyMap, "${version.foo}.${version.scala}.7", "2.0", null, false ) );

        try
        {
            PropertiesUtils.cacheProperty( project, state, propertyMap, "${version.scala}.7.${version.scala2}", "2.0", null, false );
        }
        catch (ManipulationException e)
        {
            // Pass.
        }
    }

    @Test
    public void testUpdateNestedProperties() throws Exception
    {
        Project pP = getProject();
        ManipulationSession session = createUpdateSession();

        assertTrue( updateProperties( session, pP, false, "version.hibernate.core", "5.0.4.Final-redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);

        assertTrue( updateProperties( session, pP, false, "version.scala", "2.11.7.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
        try
        {
            updateProperties( session, pP, false, "version.scala", "3.11.7-redhat-1" );
        }
        catch ( ManipulationException e )
        {
            // Pass.
        }
    }

    @Test
    public void testUpdateNestedProperties2() throws Exception
    {
        Project pP = getProject();

        ManipulationSession session = createUpdateSession();

        assertTrue( updateProperties( session, pP, false, "version.hibernate.osgi", "5.0.4.Final-redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);

        assertFalse( updateProperties( session, pP, false, "version.scala", "2.11.7" ) == PropertiesUtils.PropertyUpdate.FOUND);
    }

    @Test
    public void testUpdateNestedProperties3() throws Exception
    {
        // io.hawt:project:1.4.9
        final Model modelParent = TestUtils.resolveModelResource( RESOURCE_BASE, "project-1.4.9.pom" );
        Project pP = new Project( modelParent );

        ManipulationSession session = createUpdateSession();

        assertTrue( updateProperties( session, pP, false, "perfectus-build", "610379.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);

        try
        {
            assertTrue( updateProperties( session, pP, false, "perfectus-build", "610.NOTTHEVALUE.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
        }
        catch ( ManipulationException e )
        {
            e.printStackTrace();
            // Pass.
        }
        try
        {
            assertTrue( updateProperties( session, pP, true, "perfectus-build", "610.NOTTHEVALUE.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
        }
        catch ( ManipulationException e )
        {
            e.printStackTrace();
            // Pass.
        }
    }

    @Test
    public void testResolveProperties() throws Exception
    {
        final Model modelChild = TestUtils.resolveModelResource( RESOURCE_BASE, "inherited-properties.pom" );
        final Model modelParent = TestUtils.resolveModelResource( RESOURCE_BASE, "infinispan-bom-8.2.0.Final.pom" );
        ManipulationSession session = createUpdateSession();

        Project pP = new Project( modelParent );
        Project pC = new Project( modelChild );
        List<Project> al = new ArrayList<>();
        al.add( pC );
        al.add( pP );

        String result = PropertyResolver.resolveProperties( session, al, "${version.scala.major}.${version.scala.minor}" );
        assertTrue( result.equals( "2.11.7" ) );

        result = PropertyResolver.resolveProperties( session, al,
                                                     "TestSTART.and.${version.scala.major}.now.${version.scala.minor}" );
        assertTrue( result.equals( "TestSTART.and.2.11.now.7" ) );

        result = PropertyResolver.resolveProperties( session, al, "${project.version}" );
        assertTrue( result.equals( "1" ) );
    }

    @Test
    public void testUpdateProjectVersionProperty() throws Exception
    {
        Project pP = getProject();

        ManipulationSession session = createUpdateSession();

        assertFalse( updateProperties( session, pP, false, "project.version", "5.0.4.Final-redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
    }

    private Project getProject() throws Exception
    {
        final Model modelParent = TestUtils.resolveModelResource( RESOURCE_BASE, "infinispan-bom-8.2.0.Final.pom" );
        return new Project( modelParent );
    }

    private ManipulationSession createUpdateSession() throws Exception
    {
        ManipulationSession session = new ManipulationSession();

        Properties p = new Properties();

        p.setProperty( "strictAlignment", "true" );
        p.setProperty( "strictViolationFails", "true" );
        p.setProperty( "version.suffix", "redhat-1" );
        p.setProperty( "scanActiveProfiles", "true" );
        session.setState( new DependencyState( p ) );
        session.setState( new VersioningState( p ) );

        final MavenExecutionRequest req =
                        new DefaultMavenExecutionRequest().setUserProperties( p ).setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );

        final PlexusContainer container = new DefaultPlexusContainer();
        final MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );

        session.setMavenSession( mavenSession );

        return session;
    }

    @Test
    public void testResolvePluginsProject() throws Exception
    {
        final Model modelChild = TestUtils.resolveModelResource( RESOURCE_BASE, "inherited-properties.pom" );
        ManipulationSession session = createUpdateSession();
        Project pC = new Project( modelChild );

        assertTrue ( pC.getResolvedPlugins( session ).size() == 0);
        assertTrue ( pC.getResolvedManagedPlugins( session ).size() == 0);
    }
}
