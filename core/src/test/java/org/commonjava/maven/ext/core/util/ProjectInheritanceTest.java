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

import org.apache.maven.model.Dependency;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.io.PomIO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProjectInheritanceTest
{
    private static final String RESOURCE_BASE = "properties/";

    @Test
    public void testVerifyInheritance() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile()
                                          .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        for ( Project p : projects )
        {
            if ( ! p.getPom().equals( projectroot ) )
            {
                assertTrue ( p.getProjectParent().getPom().equals( projectroot ) );
            }
        }
    }


    @Test
    public void testVerifyInheritanceMultiple() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "integration-test/src/it/project-inheritance/pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( projectroot );

        for ( int i = 0; i<= 3 ; i++ )
        {
            if ( i == 0 )
            {
                // First project should be root
                assertTrue ( projects.get( i ).getProjectParent() == null );
                assertTrue ( projects.get( i ).getPom().equals( projectroot ) );
                assertTrue ( projects.get( i ).getInheritedList().size() == 1 );
            }
            else if ( i == 1 )
            {
                List<Project> inherited = projects.get( i ).getInheritedList();
                assertTrue (inherited.size() == 2 );
                assertTrue ( inherited.get( 1 ).getProjectParent().getPom().equals( projectroot ) );
            }
            else if ( i == 2 )
            {
                List<Project> inherited = projects.get( i ).getInheritedList();
                assertTrue (inherited.size() == 3 );
                assertTrue ( inherited.get( 0 ).getPom().equals( projectroot ) );
            }
        }
    }

    @Test
    public void testVerifyExecutionRoot() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(),
                                           "integration-test/src/it/project-inheritance/common/pom.xml" );

        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( projectroot );

        for ( Project p : projects )
        {
            if ( p.getKey().toString().equals( "io.apiman:apiman-common:1.2.7-SNAPSHOT" ))
            {
                assertTrue( p.isExecutionRoot() );
                assertTrue( p.isInheritanceRoot() );
            }
            else
            {
                assertFalse( p.isExecutionRoot() );
                assertFalse( p.isInheritanceRoot() );
            }
        }

    }

    @Test
    public void testVerifyRelativeExecutionRoot() throws Exception
    {
        final File projectRoot = new File( System.getProperty( "user.dir" ) + "/pom.xml" );

        Path root = Paths.get( projectRoot.getParent() );
        Path absolute = Paths.get( projectRoot.toString() );
        Path relative = root.relativize( absolute );

        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( relative.toFile() );

        assertTrue( projects.size() == 1 );
        assertTrue( projects.get( 0 ).isExecutionRoot() );
    }

    @Test
    public void testVerifyInheritanceReversedMultiple() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "integration-test/src/it/project-inheritance/pom.xml" );
        PomIO pomIO = new PomIO();

        List<Project> projects = pomIO.parseProject( projectroot );

        for ( int i = 0; i<= 3 ; i++ )
        {
            if ( i == 0 )
            {
                // First project should be root
                assertTrue ( projects.get( i ).getProjectParent() == null );
                assertTrue ( projects.get( i ).getPom().equals( projectroot ) );
                assertTrue ( projects.get( i ).getReverseInheritedList().size() == 1 );
            }
            else if ( i == 1 )
            {
                List<Project> inherited = projects.get( i ).getReverseInheritedList();
                assertTrue (inherited.size() == 2 );
                assertTrue ( inherited.get( 0 ).getProjectParent().getPom().equals( projectroot ) );
            }
            else if ( i == 2 )
            {
                List<Project> inherited = projects.get( i ).getReverseInheritedList();
                assertTrue (inherited.size() == 3 );
                assertTrue ( inherited.get( 2 ).getPom().equals( projectroot ) );
            }
        }
    }



    @Test
    public void testVerifyProjectVersion() throws Exception
    {
        final ManipulationSession session = new ManipulationSession();

        final File projectroot = new File (TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile()
                                                    .getParentFile(), "integration-test/src/it/project-inheritance/pom.xml" );

        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );
        for ( Project p : projects )
        {
            if ( p.getPom().equals( projectroot ) )
            {
                Map<ArtifactRef, Dependency> deps = p.getResolvedManagedDependencies( session );
                for ( ArtifactRef a : deps.keySet())
                {
                    assertFalse ( a.getVersionString().contains( "project.version" ));
                }
                deps = p.getResolvedDependencies( session );
                assertTrue( deps.size() == 1 );
                for ( ArtifactRef a : deps.keySet())
                {
                    assertFalse ( a.getVersionString().contains( "project.version" ));
                }
                assertFalse( deps.containsKey(  "org.mockito:mockito-all:jar:*" ));
                deps = p.getAllResolvedDependencies( session );
                assertTrue( deps.size() == 3 );
                for ( ArtifactRef a : deps.keySet())
                {
                    assertFalse ( a.getVersionString().contains( "project.version" ));
                    if ( a.getGroupId().equals( "org.mockito" ) )
                    {
                        assertTrue ( a.getVersionString().contains( "*" ));
                    }
                }
                assertTrue( deps.containsKey( SimpleArtifactRef.parse( "org.mockito:mockito-all:jar:*" )));
            }
        }
    }

}
