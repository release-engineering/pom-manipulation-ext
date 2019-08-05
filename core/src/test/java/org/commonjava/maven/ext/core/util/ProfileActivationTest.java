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
package org.commonjava.maven.ext.core.util;

import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationManager;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.io.PomIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileActivationTest
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String RESOURCE_BASE = "";

    @Rule
	public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    private List<Project> getProject() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = TestUtils.resolveFileResource( RESOURCE_BASE, "profile.pom" );

        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        assertEquals( 1, projects.size() );

        return projects;
    }

    @Test
    public void testVerifyProfile1() throws Exception
    {
        List<Project> p = getProject();
        ManipulationManager m = new ManipulationManager( Collections.emptyMap(), Collections.emptyMap(), null );
        ManipulationSession ms = TestUtils.createSession( null );
        m.init( ms );

        Set<String> activeProfiles = (Set<String>) TestUtils.executeMethod( m, "parseActiveProfiles",
                                                                            new Class[] { ManipulationSession.class, List.class },
                                                                            new Object[] { ms, p } );

        logger.info( "Returning active profiles of {} ", activeProfiles );

        assertEquals( 2, activeProfiles.size() );
    }

    @Test
    public void testVerifyProfile2() throws Exception
    {
        List<Project> p = getProject();
        ManipulationManager m = new ManipulationManager( Collections.emptyMap(), Collections.emptyMap(), null );
        Properties properties = new Properties(  );
        properties.setProperty( "testProperty", "testvalue" );
        ManipulationSession ms = TestUtils.createSession( properties );
        m.init( ms );

        Set<String> activeProfiles = (Set<String>) TestUtils.executeMethod( m, "parseActiveProfiles",
                                                                            new Class[] { ManipulationSession.class, List.class },
                                                                            new Object[] { ms, p } );

        activeProfiles.stream().forEach( i -> System.out.println( "Active list is " + i ) );

        assertEquals( 3, activeProfiles.size() );
        assertTrue( activeProfiles.stream().anyMatch( i -> i.equals( "testProperty" ) ) );

    }

}
