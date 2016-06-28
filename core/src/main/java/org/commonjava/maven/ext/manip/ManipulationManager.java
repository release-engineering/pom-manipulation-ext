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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.impl.Manipulator;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.ExtensionInfrastructure;
import org.commonjava.maven.ext.manip.util.ManipulatorPriorityComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates manipulation of the POMs in a build, by providing methods to read the project set from files ahead of the build proper (using
 * {@link ProjectBuilder}), then other methods to coordinate all potential {@link Manipulator} implementations (along with the {@link PomIO}
 * raw-model reader/rewriter).
 * <p>
 * Sequence of calls:
 * <ol>
 *   <li>{@link #init(ManipulationSession)}</li>
 *   <li>{@link #scan(List, ManipulationSession)}</li>
 *   <li>{@link #applyManipulations(List, ManipulationSession)}</li>
 * </ol>
 * 
 * @author jdcasey
 */
@Component( role = ManipulationManager.class )
public class ManipulationManager
{

    private static final String MARKER_PATH = "target";

    static final String MARKER_FILE =  MARKER_PATH + File.separatorChar + "pom-manip-ext-marker.txt";

    private static final Logger LOGGER = LoggerFactory.getLogger( ManipulationManager.class );

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement( role = Manipulator.class )
    private Map<String, Manipulator> manipulators;

    @Requirement( role = ExtensionInfrastructure.class )
    private Map<String, ExtensionInfrastructure> infrastructure;

    @Requirement
    private PomIO pomIO;

    /**
     * Determined from {@link Manipulator#getExecutionIndex()} comparisons during {@link #init(ManipulationSession)}.
     */
    private List<Manipulator> orderedManipulators;

    /**
     * Initialize {@link ManipulationSession} using the given {@link MavenSession} instance, along with any state managed by the individual
     * {@link Manipulator} components.
     *
     * @param session the container session for manipulation.
     * @throws ManipulationException if an error occurs.
     */
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        for ( final ExtensionInfrastructure infra : infrastructure.values() )
        {
            infra.init( session.getTargetDir(), session.getRemoteRepositories(), session.getLocalRepository(),
                        session.getSettings(), session.getActiveProfiles() );
        }

        final HashMap<Manipulator, String> revMap = new HashMap<>();
        for ( final Map.Entry<String, Manipulator> entry : manipulators.entrySet() )
        {
            revMap.put( entry.getValue(), entry.getKey() );
        }

        orderedManipulators = new ArrayList<>( revMap.keySet() );
        Collections.sort( orderedManipulators, new ManipulatorPriorityComparator() );

        for ( final Manipulator manipulator : orderedManipulators )
        {
            LOGGER.debug( "Initialising manipulator " + manipulator.getClass()
                                                                   .getSimpleName() );
            manipulator.init( session );
        }
    }

    /**
     * Encapsulates both {@link #scan(List, ManipulationSession)} and {@link #applyManipulations(List, ManipulationSession)}
     *
     * @param session the container session for manipulation.
     * @throws ManipulationException if an error occurs.
     */
    public void scanAndApply( final ManipulationSession session )
                    throws ManipulationException
    {
        final List<Project> projects = pomIO.parseProject( session.getPom() );

        scan( projects, session );

        for ( final Project project : projects )
        {
            LOGGER.debug( "Got " + project + " (POM: " + project.getPom() + ")" );
        }

        Set<Project> changed = applyManipulations( projects, session );

        // Create a marker file if we made some changes to prevent duplicate runs.
        if ( !changed.isEmpty() )
        {
            LOGGER.info( "Maven-Manipulation-Extension: Rewrite changed: " + projects );
            pomIO.rewritePOMs( changed );

            try
            {
                new File( session.getTargetDir().getParentFile(),
                          ManipulationManager.MARKER_PATH ).mkdirs();
                new File( session.getTargetDir().getParentFile(),
                          ManipulationManager.MARKER_FILE ).createNewFile();
            }
            catch ( IOException e )
            {
                throw new ManipulationException( "Marker file creation failed", e );
            }
        }

        // Ensure shutdown of GalleyInfrastructure Executor Service
        for (ExtensionInfrastructure e : infrastructure.values())
        {
            e.finish();
        }
        LOGGER.info( "Maven-Manipulation-Extension: Finished." );
    }

    /**
     * Scan the projects implied by the given POM file for modifications, and save the state in the session for later rewriting to apply it.
     *
     * @param projects the list of Projects to scan.
     * @param session the container session for manipulation.
     * @throws ManipulationException if an error occurs.
     */
    private void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
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
     *   <li>apply any manipulations from the previous {@link ManipulationManager#scan(List, ManipulationSession)} call</li>
     *   <li>rewrite any POMs that were changed</li>
     * </ul>
     *
     * @param projects the list of Projects to apply the changes to.
     * @param session the container session for manipulation.
     * @return collection of the changed projects.
     * @throws ManipulationException if an error occurs.
     */
    private Set<Project> applyManipulations( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final Set<Project> changed = new HashSet<>();
        for ( final Manipulator manipulator : orderedManipulators )
        {
            final Set<Project> mChanged = manipulator.applyChanges( projects, session );

            if ( mChanged != null )
            {
                changed.addAll( mChanged );
            }
        }

        if ( changed.isEmpty() )
        {
            LOGGER.info( "Maven-Manipulation-Extension: No changes." );
        }

        return changed;
    }

}
