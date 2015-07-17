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
package org.commonjava.maven.ext.manip;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.Manipulator;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.ExtensionInfrastructure;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.util.ManipulatorPriorityComparator;
import org.commonjava.maven.galley.maven.parse.PomPeek;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates manipulation of the POMs in a build, by providing methods to read the project set from files ahead of the build proper (using
 * {@link ProjectBuilder}), then other methods to coordinate all potential {@link Manipulator} implementations (along with the {@link PomIO}
 * raw-model reader/rewriter).
 * <br/>
 * <p>
 * Sequence of calls:
 * <ol>
 *   <li>{@link #init(MavenSession, ManipulationSession)}</li>
 *   <li>{@link #scan(File, ManipulationSession)}</li>
 *   <li>{@link #applyManipulations(List, ManipulationSession)}</li>
 * </ol>
 * 
 * @author jdcasey
 */
@Component( role = ManipulationManager.class )
public class ManipulationManager
{

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private PomIO pomIO;

    @Requirement( role = Manipulator.class )
    private Map<String, Manipulator> manipulators;

    @Requirement( role = ExtensionInfrastructure.class )
    private Map<String, ExtensionInfrastructure> infrastructure;

    /**
     * Determined from {@link Manipulator#getExecutionIndex()} comparisons during {@link #init(MavenSession, ManipulationSession)}.
     */
    private List<Manipulator> orderedManipulators;

    /**
     * Initialize {@link ManipulationSession} using the given {@link MavenSession} instance, along with any state managed by the individual
     * {@link Manipulator} components.
     */
    public void init( final MavenSession mavenSession, final ManipulationSession session )
        throws ManipulationException
    {
        session.setMavenSession( mavenSession );

        for ( final ExtensionInfrastructure infra : infrastructure.values() )
        {
            infra.init( session );
        }

        final HashMap<Manipulator, String> revMap = new HashMap<Manipulator, String>();
        for ( final Map.Entry<String, Manipulator> entry : manipulators.entrySet() )
        {
            revMap.put( entry.getValue(), entry.getKey() );
        }

        orderedManipulators = new ArrayList<Manipulator>( revMap.keySet() );
        Collections.sort( orderedManipulators, new ManipulatorPriorityComparator() );

        for ( final Manipulator manipulator : orderedManipulators )
        {
            logger.debug( "Initialising manipulator " + manipulator.getClass()
                                                                   .getSimpleName() );
            manipulator.init( session );
        }
    }

    /**
     * Scan the projects implied by the given POM file for modifications, and save the state in the session for later rewriting to apply it.
     */
    public void scan( final File pom, final ManipulationSession session )
        throws ManipulationException
    {
        final List<PomPeek> peeked = peekAtPomHierarchy( pom);
        final List<Project> projects = pomIO.readModelsForManipulation( peeked);

        session.setProjects( projects );

        for ( final Manipulator manipulator : orderedManipulators )
        {
            manipulator.scan( projects, session );
        }
    }

    /**
     * After projects are scanned for modifications, apply any modifications and rewrite POMs as needed. This method performs the following:
     * <ul>
     *   <li>read the raw models (uninherited, with only a bare minimum interpolation) from disk to escape any interpretation happening during project-building</li>
     *   <li>apply any manipulations from the previous {@link ManipulationManager#scan(File, ManipulationSession)} call</li>
     *   <li>rewrite any POMs that were changed</li>
     * </ul>
     */
    public Set<Project> applyManipulations( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final Set<Project> changed = new HashSet<Project>();
        for ( final Manipulator manipulator : orderedManipulators )
        {
            final Set<Project> mChanged = manipulator.applyChanges( projects, session );

            if ( mChanged != null )
            {
                changed.addAll( mChanged );
            }
        }

        if ( !changed.isEmpty() )
        {
            logger.info( "Maven-Manipulation-Extension: Rewrite changed: " + projects );
            pomIO.rewritePOMs( changed);
            logger.info( "Maven-Manipulation-Extension: Finished." );
        }
        else
        {
            logger.info( "Maven-Manipulation-Extension: No changes." );
        }

        return changed;
    }

    private List<PomPeek> peekAtPomHierarchy(final File topPom)
        throws ManipulationException
    {
        final List<PomPeek> peeked = new ArrayList<PomPeek>();

        try
        {
            final LinkedList<File> pendingPoms = new LinkedList<File>();
            pendingPoms.add( topPom.getCanonicalFile() );

            final String topDir = topPom.getParentFile()
                                        .getCanonicalPath();

            final Set<File> seen = new HashSet<File>();

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

                        logger.debug( "Looking for parent POM: " + parent );

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

                            logger.debug( "Looking for module POM: " + modPom );

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

            final HashSet<ProjectVersionRef> projectrefs = new HashSet<ProjectVersionRef>();

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

            logger.debug( "Searching pom list " + projectrefs.toString() + " for standalone poms..." );

            for ( final PomPeek p : peeked )
            {
                if ( p.getParentKey() == null ||
                     ! seenThisParent (projectrefs, p.getParentKey()))
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
    private boolean seenThisParent( final HashSet<ProjectVersionRef> projectrefs, final ProjectVersionRef parentKey )
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
}
