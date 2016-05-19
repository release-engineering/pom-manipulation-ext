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

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.settings.Settings;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.resolver.GalleyInfrastructure;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyInterpolatorTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void testInteropolateProperties() throws Exception
    {
        final Model model = resolveRemoteModel( "org.infinispan:infinispan-bom:8.2.0.Final" );

        Project p = new Project( model );

        Properties props = p.getModel().getProperties();
        boolean containsProperty = false;
        for ( Object o : props.values() )
        {
            if ( ( (String) o ).contains( "${" ) )
            {
                containsProperty = true;
            }
        }
        assertTrue( containsProperty );
        PropertyInterpolator pi = new PropertyInterpolator( props, p );
        assertTrue( pi.interp( "${version.hibernate.osgi}" ).equals( "5.0.4.Final" ) );
    }

    @Test
    public void testInteropolateDependencies() throws Exception
    {
        final Model model = resolveRemoteModel( "org.infinispan:infinispan-bom:8.2.0.Final" );
        Project project = new Project( model );
        PropertyInterpolator pi = new PropertyInterpolator( project.getModel().getProperties(), project );

        String nonInterp ="" , interp = "";
        for ( Dependency d : project.getManagedDependencies())
        {
            nonInterp += ( d.getGroupId().equals( "${project.groupId}" ) ? project.getGroupId() : d.getGroupId() ) + ":" +
                            ( d.getArtifactId().equals( "${project.artifactId}" ) ? project.getArtifactId() : d.getArtifactId() ) + System.lineSeparator();

            interp += pi.interp( d.getGroupId().equals( "${project.groupId}" ) ? project.getGroupId() : d.getGroupId() ) + ":" +
                                pi.interp( d.getArtifactId().equals( "${project.artifactId}" ) ? project.getArtifactId() : d.getArtifactId() ) + System.lineSeparator();

        }
        assertTrue ( nonInterp.contains( "${" ) );
        assertFalse ( interp.contains( "${" ) );
    }

    private Model resolveRemoteModel( final String resourceName ) throws Exception
    {
        List<ArtifactRepository> artifactRepos = new ArrayList<>();
        @SuppressWarnings( "deprecation" ) ArtifactRepository ar =
                        new DefaultArtifactRepository( "central", "http://central.maven.org/maven2/", new DefaultRepositoryLayout() );
        artifactRepos.add( ar );

        final GalleyInfrastructure galleyInfra =
                        new GalleyInfrastructure( temp.newFolder(), artifactRepos, null, new Settings(), Collections.<String>emptyList(), null, null, null,
                                                  temp.newFolder( "cache-dir" ) );
        final GalleyAPIWrapper wrapper = new GalleyAPIWrapper( galleyInfra );
        final ModelIO model = new ModelIO();
        FieldUtils.writeField( model, "galleyWrapper", wrapper, true );

        return model.resolveRawModel( SimpleProjectVersionRef.parse( resourceName ) );
    }
}