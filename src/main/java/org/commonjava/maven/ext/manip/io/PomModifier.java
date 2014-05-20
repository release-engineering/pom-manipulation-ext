package org.commonjava.maven.ext.manip.io;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.util.PomPeek;
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
    public static List<Project> readModelsForManipulation( Logger logger, final List<PomPeek> peeked, final ManipulationSession session )
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
                try
                {
                    in.close();
                }
                catch (IOException e) {}
            }

            if ( raw == null )
            {
                continue;
            }

            Project project = new Project( pom, raw );
            project.setTopPOM (peek.isTopPOM ());

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
    public static void rewritePOMs( Logger logger, final Set<Project> changed, final ManipulationSession session )
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
