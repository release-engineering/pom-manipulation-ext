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
package org.commonjava.maven.ext.io;

import org.apache.commons.io.FileUtils;
import org.apache.maven.io.util.DocumentModifier;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.model.io.jdom.MavenJDOMWriter;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.GAV;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.ManifestUtils;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.jdom2.Comment;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.ContentFilter;
import org.jdom2.output.LineSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Utility class used to read raw models for POMs, and rewrite any project POMs that were changed.
 *
 * @author jdcasey
 */
@Named
@Singleton
public class PomIO
{
    private static final String MODIFIED_BY = "Modified by POM Manipulation Extension for Maven";

    private static final Logger logger = LoggerFactory.getLogger( PomIO.class );


    public List<Project> parseProject (final File pom) throws ManipulationException
    {
        final List<PomPeek> peeked = peekAtPomHierarchy(pom);
        return readModelsForManipulation( pom.getAbsoluteFile(), peeked );
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
        final HashMap<Project, ProjectVersionRef> projectToParent = new HashMap<>(  );

        for ( final PomPeek peek : peeked )
        {
            final File pom = peek.getPom();

            // Sucks, but we have to brute-force reading in the raw model.
            // The effective-model building, below, has a tantalizing getRawModel()
            // method on the result, BUT this seems to return models that have
            // the plugin versions set inside profiles...so they're not entirely
            // raw.
            Model raw;
            try ( InputStream in = new FileInputStream( pom ) )
            {
                raw = new MavenXpp3Reader().read( in );
            }
            catch ( final IOException | XmlPullParserException e )
            {
                throw new ManipulationException( "Failed to build model for POM: %s.\n--> %s", e, pom, e.getMessage() );
            }

            if ( raw == null )
            {
                continue;
            }

            final Project project = new Project( pom, raw );
            projectToParent.put( project, peek.getParentKey() );
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

        // Fill out inheritance info for every project we have created.
        for ( Project p : projects )
        {
            ProjectVersionRef pvr = projectToParent.get( p );
            p.setProjectParent( getParent( projects, pvr ) );
        }

        return projects;
    }

    private Project getParent( List<Project> projects, ProjectVersionRef pvr )
    {
        for ( Project p : projects )
        {
            if ( p.getKey().equals( pvr ) )
            {
                return p;
            }
        }
        // If the PVR refers to something outside of the hierarchy we'll break the inheritance here.
        return null;
    }

    /**
     * For any project listed as changed (tracked by GA in the session), write the modified model out to disk.
     * Uses JDOM {@link ModelWriter} and {@link MavenJDOMWriter} to preserve as much formatting as possible.
     *
     *
     * @param changed the modified Projects to write out.
     * @return gav execution root GAV
     * @throws ManipulationException if an error occurs.
     */
    public GAV rewritePOMs( final Set<Project> changed )
        throws ManipulationException
    {
        GAV result = null;

        for ( final Project project : changed )
        {
            if ( project.isExecutionRoot() )
            {
                result = new GAV( project.getKey() );
            }
            logger.debug( String.format( "%s modified! Rewriting.", project ) );
            File pom = project.getPom();

            final Model model = project.getModel();
            logger.trace( "Rewriting: " + model.getId() + " in place of: " + project.getKey()
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
        return result;
    }


    /**
     * Writes out the Model to the selected target file.
     *
     * @param model the Model to write out.
     * @param target the file to write to.
     * @throws ManipulationException if an error occurs.
     */
    public void writeModel( final Model model, final File target)
                    throws ManipulationException
    {
        try
        {
            new MavenXpp3Writer().write( new FileWriter( target ), model );
        }
        catch ( IOException e )
        {
            throw new ManipulationException( "Unable to write file", e );
        }
    }

    private void write( final Project project, final File pom, final Model model )
        throws ManipulationException
    {
        try
        {
            final String manifestInformation = project.isInheritanceRoot() ? ManifestUtils.getManifestInformation() : null;

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

     * @param projectrefs GAVs to search
     * @param parentKey Key to find
     * @return whether its been found
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
