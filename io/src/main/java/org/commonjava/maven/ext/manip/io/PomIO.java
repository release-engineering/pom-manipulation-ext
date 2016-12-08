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
package org.commonjava.maven.ext.manip.io;

import org.apache.commons.io.FileUtils;
import org.apache.maven.io.util.DocumentModifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.GAV;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.jdom2.output.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.Manifest;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Utility class used to read raw models for POMs, and rewrite any project POMs that were changed.
 *
 * @author jdcasey
 */
@Component( role = PomIO.class )
public class PomIO
{
    private static final String MODIFIED_BY = "Modified by POM Manipulation Extension for Maven";

    private static final Logger logger = LoggerFactory.getLogger( PomIO.class );


    public List<Project> parseProject (final File pom) throws ManipulationException
    {
        final List<PomPeek> peeked = peekAtPomHierarchy(pom);
        return readModelsForManipulation( pom, peeked );
    }

    /**
     * Read {@link Model} instances by parsing the POM directly. This is useful to escape some post-processing that happens when the
     * {@link MavenProject#getOriginalModel()} instance is set.
     *
     * @param executionRoot the top level pom file.
     * @param peeked a collection of poms resolved from the top level file.
     * @return a collection of Projects
     * @throws ManipulationException if an error occurs.
     */
    private List<Project> readModelsForManipulation( File executionRoot, final List<PomPeek> peeked )
        throws ManipulationException
    {
        final List<Project> projects = new ArrayList<>();

        for ( final PomPeek peek : peeked )
        {
            final File pom = peek.getPom();

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
            catch ( final IOException | XmlPullParserException e )
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

            if ( executionRoot.equals( pom ))
            {
                logger.debug( "Setting execution root to {} with file {}" +
                      (project.isInheritanceRoot() ? " and is the inheritance root. ": ""), project, pom );
                project.setExecutionRoot ();

                try
                {
                    if ( FileUtils.readFileToString( pom ).contains( MODIFIED_BY ) )
                    {
                        project.setIncrementalPME (true);
                    }
                }
                catch ( final IOException e )
                {
                    throw new ManipulationException( "Failed to read POM: %s", e, pom );
                }
            }

            projects.add( project );
        }

        return projects;
    }

    /**
     * For any project listed as changed (tracked by GA in the session), write the modified model out to disk.
     * Uses JDOM {@link ModelWriter} and {@link MavenJDOMWriter} to preserve as much formatting as possible.
     *
     *
     * @param gav GAV to fill in.
     * @param changed the modified Projects to write out.
     * @throws ManipulationException if an error occurs.
     */
    public void rewritePOMs( GAV gav, final Set<Project> changed )
        throws ManipulationException
    {
        for ( final Project project : changed )
        {
            if ( project.isExecutionRoot() )
            {
                Model m = project.getModel();
                gav.setGroupId( m.getGroupId() == null ? project.getGroupId() : m.getGroupId() );
                gav.setArtifactId( m.getArtifactId() == null ? project.getArtifactId() : m.getArtifactId() );
                gav.setVersion( m.getVersion() == null ? project.getVersion() : m.getVersion() );
            }
            logger.info( String.format( "%s modified! Rewriting.", project ) );
            File pom = project.getPom();

            final Model model = project.getModel();
            logger.trace( "Rewriting: " + model.toString() + " in place of: " + project.getId()
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
            final String manifestInformation = project.isInheritanceRoot() ? getManifestInformation() : null;

            MavenJDOMWriter mjw = new MavenJDOMWriter( model );

            // We possibly could store the EOL type in the Project when we first read
            // the file but we would then have to do a dual read, then write as opposed
            // to a read, then read + write now.
            LineSeparator ls = determineEOL( pom );
            mjw.setLineSeparator( ls );

            mjw.write( model, pom, new DocumentModifier()
            {
                @Override
                public void postProcess( final Document doc )
                {
                    // Only add the modified by to the top level pom.
                    if ( project.isExecutionRoot() )
                    {
                        final Iterator<Content> it = doc.getContent( new ContentFilter( ContentFilter.COMMENT ) )
                                                        .iterator();
                        while ( it.hasNext() )
                        {
                            final Comment c = (Comment) it.next();

                            if ( c.toString().contains( MODIFIED_BY ) )
                            {
                                it.remove();
                            }
                        }

                        doc.addContent( Collections.<Content>singletonList(
                                        new Comment( "\nModified by POM Manipulation Extension for Maven "
                                                                     + manifestInformation + "\n" ) ) );
                    }
                }
            });
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

    /**
     * Retrieves the SHA this was built with.
     *
     * @return the GIT sha of this codebase.
     * @throws ManipulationException if an error occurs.
     */
    public static String getManifestInformation()
        throws ManipulationException
    {
        String result = "";
        try
        {
            final Enumeration<URL> resources = PomIO.class.getClassLoader()
                                                                         .getResources( "META-INF/MANIFEST.MF" );

            while ( resources.hasMoreElements() )
            {
                final URL jarUrl = resources.nextElement();

                if ( jarUrl.getFile()
                           .contains( "pom-manipulation-" ) )
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

    private List<PomPeek> peekAtPomHierarchy(final File topPom)
        throws ManipulationException
    {
        final List<PomPeek> peeked = new ArrayList<>();

        try
        {
            final LinkedList<File> pendingPoms = new LinkedList<>();
            pendingPoms.add( topPom.getCanonicalFile() );

            final String topDir = topPom.getAbsoluteFile().getParentFile().getCanonicalPath();

            final Set<File> seen = new HashSet<>();

            File topLevelParent = topPom;

            while ( !pendingPoms.isEmpty() )
            {
                final File pom = pendingPoms.removeFirst();
                seen.add( pom );

                logger.debug( "PEEK: " + pom );

                final PomPeek peek = new PomPeek( pom );
                final ProjectVersionRef key = peek.getKey();
                if ( key != null )
                {
                    peeked.add( peek );

                    final File dir = pom.getParentFile();

                    final String relPath = peek.getParentRelativePath();
                    if ( relPath != null )
                    {
                        logger.debug( "Found parent relativePath: " + relPath + " in pom: " + pom );
                        File parent = new File( dir, relPath );
                        if ( parent.isDirectory() )
                        {
                            parent = new File( parent, "pom.xml" );
                        }

                        parent = parent.getCanonicalFile();
                        if ( parent.getParentFile()
                                   .getCanonicalPath()
                                   .startsWith( topDir ) && parent.exists() && !seen.contains( parent )
                            && !pendingPoms.contains( parent ) )
                        {
                            topLevelParent = parent;
                            logger.debug( "Possible top level parent " + parent );
                            pendingPoms.add( parent );
                        }
                        else
                        {
                            logger.debug( "Skipping reference to non-existent parent relativePath: '" + relPath
                                + "' in: " + pom );
                        }
                    }

                    final Set<String> modules = peek.getModules();
                    if ( modules != null && !modules.isEmpty() )
                    {
                        for ( final String module : modules )
                        {
                            logger.debug( "Found module: " + module + " in pom: " + pom );

                            File modPom = new File( dir, module );
                            if ( modPom.isDirectory() )
                            {
                                modPom = new File( modPom, "pom.xml" );
                            }

                            if ( modPom.exists() && !seen.contains( modPom )
                                && !pendingPoms.contains( modPom ) )
                            {
                                pendingPoms.addLast( modPom );
                            }
                            else
                            {
                                logger.debug( "Skipping reference to non-existent module: '" + module + "' in: " + pom );
                            }
                        }
                    }
                }
                else
                {
                    logger.debug( "Skipping " + pom + " as its a template file." );
                }
            }

            final HashSet<ProjectVersionRef> projectrefs = new HashSet<>();

            for ( final PomPeek p : peeked )
            {
                projectrefs.add( p.getKey() );

                if ( p.getPom()
                      .equals( topLevelParent ) )
                {
                    logger.debug( "Setting top level parent to " + p.getPom() + " :: " + p.getKey() );
                    p.setInheritanceRoot( true );
                }
            }

            for ( final PomPeek p : peeked )
            {
                if ( p.getParentKey() == null ||
                     ! seenThisParent(projectrefs, p.getParentKey()))
                {
                    logger.debug( "Found a standalone pom " + p.getPom() + " :: " + p.getKey() );
                    p.setInheritanceRoot( true );
                }
            }
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Problem peeking at POMs.", e );
        }

        return peeked;
    }

    /**
     * Search the list of project references to establish if this parent reference exists in them. This
     * determines whether the module is inheriting something inside the project or an external reference.

     * @param projectrefs
     * @param parentKey
     * @return
     */
    private boolean seenThisParent(final HashSet<ProjectVersionRef> projectrefs, final ProjectVersionRef parentKey)
    {
        for (final ProjectVersionRef p : projectrefs)
        {
            if ( p.versionlessEquals( parentKey ))
            {
                return true;
            }
        }
        return false;
    }


    private static LineSeparator determineEOL( File pom )
        throws ManipulationException
    {
        try (  BufferedInputStream bufferIn = new BufferedInputStream( new FileInputStream( pom ) ) )
        {
            int prev = -1;
            int ch;
            while ( ( ch = bufferIn.read() ) != -1 )
            {
                if ( ch == '\n' )
                {
                    if ( prev == '\r' )
                    {
                        return LineSeparator.CRNL;
                    }
                    else
                    {
                        return LineSeparator.NL;
                    }
                }
                else if ( prev == '\r' )
                {
                    return LineSeparator.CR;
                }
                prev = ch;
            }
            throw new ManipulationException( "Could not determine end-of-line marker mode" );
        }
        catch ( IOException ioe )
        {
            throw new ManipulationException( "Could not determine end-of-line marker mode", ioe );
        }
    }
}
