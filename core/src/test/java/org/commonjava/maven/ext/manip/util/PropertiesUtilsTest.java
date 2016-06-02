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

import org.apache.maven.model.Model;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.fixture.TestUtils;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
        ManipulationSession session = new ManipulationSession();
        assertFalse( PropertiesUtils.checkStrictValue( session, null, "1.0" ) );
        assertFalse( PropertiesUtils.checkStrictValue( session, "1.0", null ) );
    }

    @Test
    public void testCacheProperty() throws Exception
    {
        Map<String,String> propertyMap = new HashMap<>();

        assertFalse( PropertiesUtils.cacheProperty( null, "${foobar}${foobar2}", null, null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( null, "suffix.${foobar}", null, null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( propertyMap, null, "2.0", null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( propertyMap, "1.0", "2.0", null, false ) );
        assertTrue( PropertiesUtils.cacheProperty( propertyMap, "${version.org.jboss}", "2.0", null, false ) );
        assertFalse ( PropertiesUtils.cacheProperty( propertyMap, "${project.version}", "2.0", null, false ) );

        // DependencyManipulator does dependency.getVersion(). This could return e.g. ${version.scala} which can
        // refer to <version.scala>${version.scala.major}.7</version.scala>. If we are attempting to change version.scala
        // to e.g. 2.11.7.redhat-1 then in this case we need to ignore the version.scala.major property and append the .redhat-1.

        // If the property is ${...}.foobar then we only want to append suffix to foobar to change the version
        // However we don't need to change the value of the property. If the property is foobar.${....} then
        // we want to append suffix to the property ... but we need to handle that part of the property is hardcoded.

        assertFalse( PropertiesUtils.cacheProperty( propertyMap, "${version.scala}.7", "2.0", null, false ) );
        assertFalse( PropertiesUtils.cacheProperty( propertyMap, "${version.foo}.${version.scala}.7", "2.0", null, false ) );

        try
        {
            PropertiesUtils.cacheProperty( propertyMap, "${version.scala}.7.${version.scala2}", "2.0", null, false );
        }
        catch (ManipulationException e)
        {
            // Pass.
        }
    }

    private ManipulationSession createUpdateSession()
    {
        ManipulationSession session = new ManipulationSession();
        Properties p = new Properties();
        p.setProperty( "strictAlignment", "true" );
        p.setProperty( "strictViolationFails", "true" );
        p.setProperty( "version.suffix", "redhat-1" );
        session.setState( new DependencyState( p ) );
        session.setState( new VersioningState( p ) );

        return session;
    }

    @Test
    public void testUpdateNestedProperties() throws Exception
    {
        final Model modelParent = TestUtils.resolveModelResource( RESOURCE_BASE, "infinispan-bom-8.2.0.Final.pom" );
        Project pP = new Project( modelParent );
        Set<Project> sp = new HashSet<>();
        sp.add( pP );

        ManipulationSession session = createUpdateSession();

        assertTrue( updateProperties( session, sp, false, "version.hibernate.core", "5.0.4.Final-redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);

        assertTrue( updateProperties( session, sp, false, "version.scala", "2.11.7.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
        try
        {
            updateProperties( session, sp, false, "version.scala", "3.11.7-redhat-1" );
        }
        catch ( ManipulationException e )
        {
            // Pass.
        }
    }

    @Test
    public void testUpdateNestedProperties2() throws Exception
    {
        final Model modelParent = TestUtils.resolveModelResource(  RESOURCE_BASE, "infinispan-bom-8.2.0.Final.pom" );
        Project pP = new Project( modelParent );
        Set<Project> sp = new HashSet<>();
        sp.add( pP );

        ManipulationSession session = createUpdateSession();

        assertTrue( updateProperties( session, sp, false, "version.hibernate.osgi", "5.0.4.Final-redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);

        assertFalse( updateProperties( session, sp, false, "version.scala", "2.11.7" ) == PropertiesUtils.PropertyUpdate.FOUND);
    }

    @Test
    public void testUpdateNestedProperties3() throws Exception
    {
        // io.hawt:project:1.4.9
        final Model modelParent = TestUtils.resolveModelResource( RESOURCE_BASE, "project-1.4.9.pom" );
        Project pP = new Project( modelParent );
        Set<Project> sp = new HashSet<>();
        sp.add( pP );

        ManipulationSession session = createUpdateSession();

        assertTrue( updateProperties( session, sp, false, "perfectus-build", "610379.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);

        try
        {
            assertTrue( updateProperties( session, sp, false, "perfectus-build", "610.NOTTHEVALUE.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
        }
        catch ( ManipulationException e )
        {
            e.printStackTrace();
            // Pass.
        }
        try
        {
            assertTrue( updateProperties( session, sp, true, "perfectus-build", "610.NOTTHEVALUE.redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
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

        Project pP = new Project( modelParent );
        Project pC = new Project( modelChild );
        List<Project> al = new ArrayList<>();
        al.add( pC );
        al.add( pP );

        String result = PropertiesUtils.resolveProperties( al, "${version.scala.major}.${version.scala.minor}" );
        assertTrue( result.equals( "2.11.7" ) );

        result = PropertiesUtils.resolveProperties( al,
                                                    "TestSTART.and.${version.scala.major}.now.${version.scala.minor}" );
        assertTrue( result.equals( "TestSTART.and.2.11.now.7" ) );

        result = PropertiesUtils.resolveProperties( al, "${project.version}" );
        assertTrue( result.equals( "1" ) );
   }

    @Test
    public void testUpdateProjectVersionProperty() throws Exception
    {
        final Model modelParent = TestUtils.resolveModelResource( RESOURCE_BASE, "infinispan-bom-8.2.0.Final.pom" );
        Project pP = new Project( modelParent );
        Set<Project> sp = new HashSet<>();
        sp.add( pP );

        ManipulationSession session = createUpdateSession();

        assertFalse( updateProperties( session, sp, false, "project.version", "5.0.4.Final-redhat-1" ) == PropertiesUtils.PropertyUpdate.FOUND);
    }
}
