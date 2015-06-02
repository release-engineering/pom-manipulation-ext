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
package org.commonjava.maven.ext.manip.io;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.maven.io.util.DocumentModifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.ext.manip.ManipulatingEventSpy;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.util.PomPeek;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class used to read raw models for POMs, and rewrite any project POMs that were changed.
 *
 * @author jdcasey
 */
@Component( role = PomIO.class )
public class PomIO
{

    private static final String MODIFIED_BY = "[Comment: <!-- Modified by POM Manipulation Extension for Maven";

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    protected PomIO()
    {
    }

    /**
     * Read {@link Model} instances by parsing the POM directly. This is useful to escape some post-processing that happens when the
     * {@link MavenProject#getOriginalModel()} instance is set.
     */
    public List<Project> readModelsForManipulation( final List<PomPeek> peeked, final ManipulationSession session )
        throws ManipulationException
    {
        final List<Project> projects = new ArrayList<Project>();

        for ( final PomPeek peek : peeked )
        {
            final File pom = peek.getPom();

            logger.debug( "Reading raw model for: " + pom );

            // Sucks, but we have to brute-force reading in the raw model.
            // The effective-model building, below, has a tantalizing getRawModel()
            // method on the result, BUT this seems to return models that have
            // the plugin versions set inside profiles...so they're not entirely
            // raw.
            Model raw = null;
            InputStream in = null;
            try
            {
                in = new FileInputStream( pom );
                raw = new MavenXpp3Reader().read( in );
            }
            catch ( final IOException e )
            {
                throw new ManipulationException( "Failed to build model for POM: %s.\n--> %s", e, pom, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                throw new ManipulationException( "Failed to build model for POM: %s.\n--> %s", e, pom, e.getMessage() );
            }
            finally
            {
                closeQuietly( in );
            }

            if ( raw == null )
            {
                continue;
            }

            final Project project = new Project( pom, raw );
            project.setInheritanceRoot( peek.isInheritanceRoot() );

            projects.add( project );
        }

        return projects;
    }

    /**
     * For any project listed as changed (tracked by GA in the session), write the modified model out to disk. Uses JDOM {@link ModelWriter}
     * ({@MavenJDOMWriter}) to preserve as much formatting as possible.
     * @param logger
     */
    public void rewritePOMs( final Set<Project> changed, final ManipulationSession session )
        throws ManipulationException
    {
        for ( final Project project : changed )
        {
            logger.info( String.format( "%s modified! Rewriting.", project ) );
            File pom = project.getPom();

            final Model model = project.getModel();
            logger.info( "Rewriting: " + model.toString() + " in place of: " + project.getId()
                         + "\n       to POM: " + pom );

            write( project, pom, model );

            // this happens with integration tests!
            // This is a total hack, but the alternative seems to be adding complexity through a custom model processor.
            if ( pom.getName()
                            .equals( "interpolated-pom.xml" ) )
            {
                final File dir = pom.getParentFile();
                pom = dir == null ? new File( "pom.xml" ) : new File( dir, "pom.xml" );

                write( project, pom, model );
            }
        }
    }

    private void write( final Project project, final File pom, final Model model )
        throws ManipulationException
    {
        try
        {
            final MavenJDOMWriter writer = new MavenJDOMWriter( model );

            final String manifestInformation = project.isInheritanceRoot() ? getManifestInformation() : null;
            new MavenJDOMWriter().write( model, pom, new DocumentModifier()
            {
                @Override
                public void postProcess( final Document doc )
                {
                    // Only add the modified by to the top level pom.
                    if ( project.isInheritanceRoot() )
                    {
                        final Iterator<Content> it = doc.getContent( new ContentFilter( ContentFilter.COMMENT ) )
                                                        .iterator();
                        while ( it.hasNext() )
                        {
                            final Comment c = (Comment) it.next();

                            //final Comment c = (Comment) it.next();
                            if ( c.toString()
                                  .startsWith( MODIFIED_BY ) )
                            {
                                it.remove();

                                break;
                            }
                        }

                        doc.addContent( Arrays.<Content> asList( new Comment(
                                                                              "\nModified by POM Manipulation Extension for Maven "
                                                                                  + manifestInformation + "\n" ) ) );
                    }
                }
            } );
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Failed to read POM for rewrite: %s. Reason: %s", e, pom, e.getMessage() );
        }
        catch ( final JDOMException e )
        {
            throw new ManipulationException( "Failed to parse POM for rewrite: %s. Reason: %s", e, pom, e.getMessage() );
        }
    }

    private String getManifestInformation()
        throws ManipulationException
    {
        String result = "";
        try
        {
            final Enumeration<URL> resources = ManipulatingEventSpy.class.getClassLoader()
                                                                         .getResources( "META-INF/MANIFEST.MF" );

            while ( resources.hasMoreElements() )
            {
                final URL jarUrl = resources.nextElement();

                logger.debug( "Processing jar resource " + jarUrl );
                if ( jarUrl.getFile()
                           .contains( "pom-manipulation-ext" ) )
                {
                    final Manifest manifest = new Manifest( jarUrl.openStream() );
                    result = manifest.getMainAttributes()
                                     .getValue( "Implementation-Version" );
                    result += " ( SHA: " + manifest.getMainAttributes()
                                                   .getValue( "Scm-Revision" ) + " ) ";
                    break;
                }
            }
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Error retrieving information from manifest", e );
        }

        return result;
    }
}
