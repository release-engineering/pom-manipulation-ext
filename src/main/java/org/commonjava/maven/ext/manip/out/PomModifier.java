package org.commonjava.maven.ext.manip.out;

import static org.commonjava.maven.ext.manip.IdUtils.ga;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.Format.TextMode;

/**
 * Utility class used to read raw models for POMs, and rewrite any project POMs that were changed.
 *
 * @author jdcasey
 */
public final class PomModifier
{

    private PomModifier()
    {
    }

    /**
     * Read {@link Model} instances by parsing the POM directly. This is useful to escape some post-processing that happens when the
     * {@link MavenProject#getOriginalModel()} instance is set.
     * @param logger
     */
    public static void readModelsForManipulation( Logger logger, final List<MavenProject> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final Map<String, Model> rawModels = new HashMap<String, Model>();
        for ( final MavenProject project : projects )
        {
            final String ga = ga( project );
            final File pom = project.getFile();

            // this happens with integration tests!
            //            if ( pom.getName()
            //                    .equals( "interpolated-pom.xml" ) )
            //            {
            //                final File dir = pom.getParentFile();
            //                pom = dir == null ? new File( "pom.xml" ) : new File( dir, "pom.xml" );
            //            }

            logger.debug( "Reading raw model for: " + project.getId() + "\n       to POM: " + pom );

            Reader pomReader = null;
            Model model;
            try
            {
                pomReader = ReaderFactory.newXmlReader( pom );
                model = new MavenXpp3Reader().read( pomReader );
            }
            catch ( final IOException e )
            {
                throw new ManipulationException( "Failed to read POM: %s. Reason: %s", e, pom, e.getMessage() );
            }
            catch ( final XmlPullParserException e )
            {
                throw new ManipulationException( "Failed to parse POM: %s. Reason: %s", e, pom, e.getMessage() );
            }
            finally
            {
                IOUtil.close( pomReader );
            }

            rawModels.put( ga, model );
        }

        session.setManipulatedModels( rawModels );
    }

    /**
     * For any project listed as changed (tracked by GA in the session), write the modified model out to disk. Uses JDOM {@link ModelWriter}
     * ({@MavenJDOMWriter}) to preserve as much formatting as possible.
     * @param logger
     */
    public static void rewritePOMs( Logger logger, final Collection<MavenProject> projects, final ManipulationSession session )
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

            for ( final MavenProject project : projects )
            {
                final String ga = ga( project );
                logger.info( String.format( "%s modified! Rewriting.", project ) );
                File pom = project.getFile();

                final Model model = modifiedModels.get( ga );
                logger.info( "Rewriting: " + model.toString() + " in place of: " + project.getId() + "\n       to POM: " + pom );

                write( pom, model );

                // this happens with integration tests!
                // This is a total hack, but the alternative seems to be adding complexity through a custom model processor.
                if ( pom.getName()
                        .equals( "interpolated-pom.xml" ) )
                {
                    final File dir = pom.getParentFile();
                    pom = dir == null ? new File( "pom.xml" ) : new File( dir, "pom.xml" );

                    write( pom, model );
                }

                pw.println( project.getId() );
            }
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Failed to open output log file: %s. Reason: %s", e, marker, e.getMessage() );
        }
        finally
        {
            IOUtil.close( pw );
        }
    }

    private static void write( final File pom, final Model model )
        throws ManipulationException
    {
        Writer pomWriter = null;
        try
        {
            final SAXBuilder builder = new SAXBuilder();
            final Document doc = builder.build( pom );

            String encoding = model.getModelEncoding();
            if ( encoding == null )
            {
                encoding = "UTF-8";
            }

            final Format format = Format.getRawFormat()
                                        .setEncoding( encoding )
                                        .setTextMode( TextMode.PRESERVE )
                                        .setLineSeparator( System.getProperty( "line.separator" ) )
                                        .setOmitDeclaration( false )
                                        .setOmitEncoding( false )
                                        .setExpandEmptyElements( true );

            pomWriter = WriterFactory.newWriter( pom, encoding );
            new MavenJDOMWriter().write( model, doc, pomWriter, format );

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
            IOUtil.close( pomWriter );
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

}
