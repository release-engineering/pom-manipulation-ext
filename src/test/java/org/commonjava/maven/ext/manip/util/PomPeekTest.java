/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.commonjava.maven.ext.manip.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.model.Project;
import org.junit.Test;


public class PomPeekTest
{

    private static final String BASE = "pom-peek/";

    @Test
    public void findModules()
    {
        final File pom = getResourceFile( BASE + "contains-modules.pom" );
        final PomPeek peek = new PomPeek( pom );
        final Set<String> modules = peek.getModules();
        assertThat( modules, notNullValue() );
        assertThat( modules.size(), equalTo( 2 ) );
        assertThat( modules.contains( "child1" ), equalTo( true ) );
        assertThat( modules.contains( "child2" ), equalTo( true ) );
        assertThat( modules.contains( "child3" ), equalTo( false ) );
    }

    @Test
    public void findGAVDirectlyInProjectAtTop()
    {
        final File pom = getResourceFile( BASE + "direct-gav-at-top.pom" );
        final PomPeek peek = new PomPeek( pom );

        assertThat( peek.getKey(), notNullValue() );

        final ProjectVersionRef key = peek.getKey();
        assertThat( key.getGroupId(), equalTo( "test" ) );
        assertThat( key.getArtifactId(), equalTo( "direct-gav-at-top" ) );
        assertThat( key.getVersionString(), equalTo( "1" ) );

    }

    @Test
    public void findGAVDirectlyInProjectBelowProperties()
    {
        final File pom = getResourceFile( BASE + "direct-gav-below-props.pom" );
        final PomPeek peek = new PomPeek( pom );

        assertThat( peek.getKey(), notNullValue() );

        final ProjectVersionRef key = peek.getKey();
        assertThat( key.getGroupId(), equalTo( "test" ) );
        assertThat( key.getArtifactId(), equalTo( "direct-gav-below-props" ) );
        assertThat( key.getVersionString(), equalTo( "1" ) );

    }

    @Test
    public void findGAVInheritedFromParentAtTop()
    {
        final File pom = getResourceFile( BASE + "inherited-gav-at-top.pom" );
        final PomPeek peek = new PomPeek( pom );

        assertThat( peek.getKey(), notNullValue() );

        final ProjectVersionRef key = peek.getKey();
        assertThat( key.getGroupId(), equalTo( "test" ) );
        assertThat( key.getArtifactId(), equalTo( "inherited-gav-at-top" ) );
        assertThat( key.getVersionString(), equalTo( "1" ) );

    }

    @Test
    public void findGAVInheritedFromParentWithVersionOverrideAtTop()
    {
        final File pom = getResourceFile( BASE + "inherited-gav-with-override-at-top.pom" );
        final PomPeek peek = new PomPeek( pom );

        assertThat( peek.getKey(), notNullValue() );

        final ProjectVersionRef key = peek.getKey();
        assertThat( key.getGroupId(), equalTo( "test" ) );
        assertThat( key.getArtifactId(), equalTo( "inherited-gav-with-override-at-top" ) );
        assertThat( key.getVersionString(), equalTo( "2" ) );

    }

    @Test
    public void findGAVInheritedFromParentWithGroupAndVersionOverrideAtTop() throws Exception
    {
        final File pom = getResourceFile( BASE + "inherited-gav-with-group-override-at-top.pom" );
        final PomPeek peek = new PomPeek( pom );

        final InputStream in = new FileInputStream( pom );
        final Model raw = new MavenXpp3Reader().read( in );
        final Project project = new Project( pom, raw );

        assertThat( peek.getKey(), notNullValue() );

        assertThat( project.getGroupId(), equalTo( "a-different-test-group" ) );
        assertThat( project.getArtifactId(), equalTo( "inherited-gav-with-group-override-at-top" ) );
        assertThat( project.getVersion(), equalTo( "1" ) );

    }

    // Utility functions

    public static File getResourceFile( final String path )
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( path );
        if ( resource == null )
        {
            // fail( "Resource not found: " + path );
        }

        return new File( resource.getPath() );
    }


}
