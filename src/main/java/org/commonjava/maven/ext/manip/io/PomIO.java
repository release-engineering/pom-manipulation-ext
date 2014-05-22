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
package org.commonjava.maven.ext.manip.io;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.ext.manip.ManipulatingEventSpy;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.util.PomPeek;
import org.jdom.Comment;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.filter.ContentFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;
import org.jdom.output.XMLOutputter;

/**
 * Utility class used to read raw models for POMs, and rewrite any project POMs that were changed.
 *
 * @author jdcasey
 */
@Component( role = PomIO.class )
public class PomIO
{

    @Requirement
    protected Logger logger;

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
        final Map<String, Model> rawModels = new HashMap<String, Model>();

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
            project.setTopPOM( peek.isTopPOM() );

            rawModels.put( ga( project ), raw );
            projects.add( project );
        }
        session.setManipulatedModels( rawModels );

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
        final Map<String, Model> modifiedModels = session.getManipulatedModels();

        final File marker = getMarkerFile( session );
        PrintWriter pw = null;
        try
        {
            marker.getParentFile()
                  .mkdirs();

            pw = new PrintWriter( new FileWriter( marker ) );

            for ( final Project project : changed )
            {
                final String ga = ga( project );
                logger.info( String.format( "%s modified! Rewriting.", project ) );
                File pom = project.getPom();

                final Model model = modifiedModels.get( ga );
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

                pw.println( project.getId() );
            }
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Failed to open output log file: %s. Reason: %s", e, marker,
                                             e.getMessage() );
        }
        finally
        {
            closeQuietly( pw );
        }
    }

    private void write( final Project project, final File pom, final Model model )
        throws ManipulationException
    {
        Writer pomWriter = null;
        try
        {
            final SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build( pom );

            String encoding = model.getModelEncoding();
            if ( encoding == null )
            {
                encoding = "UTF-8";
            }
            final String modifiedBy = "[Comment: <!-- Modified by POM Manipulation Extension for Maven";

            final Format format = Format.getRawFormat()
                                        .setEncoding( encoding )
                                        .setTextMode( TextMode.PRESERVE )
                                        .setLineSeparator( System.getProperty( "line.separator" ) )
                                        .setOmitDeclaration( false )
                                        .setOmitEncoding( false )
                                        .setExpandEmptyElements( true );

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pomWriter = WriterFactory.newWriter( baos, encoding );

            new MavenJDOMWriter().write( model, doc, pomWriter, format );
            doc = builder.build( new ByteArrayInputStream( baos.toByteArray() ) );

            // Only add the modified by to the top level pom.
            if ( project.isTopPOM() )
            {
                @SuppressWarnings( "unchecked" )
                final Iterator<Comment> it = doc.getContent( new ContentFilter( ContentFilter.COMMENT ) )
                                                .iterator();
                while ( it.hasNext() )
                {
                    final Comment c = it.next();

                    //final Comment c = (Comment) it.next();
                    if ( c.toString()
                          .startsWith( modifiedBy ) )
                    {
                        it.remove();

                        break;
                    }
                }
                doc.addContent( new Comment( " Modified by POM Manipulation Extension for Maven "
                    + getManifestInformation() ) );
            }
            final List<?> rootComments = doc.getContent( new ContentFilter( ContentFilter.COMMENT ) );

            final XMLOutputter xmlo = new XMLOutputter( format )
            {
                @Override
                protected void printComment( final Writer out, final Comment comment )
                    throws IOException
                {
                    if ( comment.toString()
                                .startsWith( modifiedBy ) )
                    {
                        out.write( getFormat().getLineSeparator() );
                    }

                    super.printComment( out, comment );

                    // If root level comments exist and is the current Comment object
                    // output an extra newline to tidy the output
                    if ( rootComments.contains( comment ) )
                    {
                        out.write( System.getProperty( "line.separator" ) );
                    }
                }
            };

            pomWriter = WriterFactory.newWriter( pom, encoding );

            xmlo.output( doc, pomWriter );

            pomWriter.flush();
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Failed to read POM for rewrite: %s. Reason: %s", e, pom, e.getMessage() );
        }
        catch ( final JDOMException e )
        {
            throw new ManipulationException( "Failed to parse POM for rewrite: %s. Reason: %s", e, pom, e.getMessage() );
        }
        finally
        {
            closeQuietly( pomWriter );
        }
    }

    private static File getMarkerFile( final ManipulationSession session )
    {
        final File pom = session.getRequest()
                                .getPom();

        File markerFile;
        if ( pom != null )
        {
            File dir = pom.getParentFile();
            if ( dir == null )
            {
                dir = pom.getAbsoluteFile()
                         .getParentFile();
            }

            markerFile = new File( dir, "target/manipulation.log" );
        }
        else
        {
            markerFile = new File( "target/manipulation.log" );
        }

        return markerFile;
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
