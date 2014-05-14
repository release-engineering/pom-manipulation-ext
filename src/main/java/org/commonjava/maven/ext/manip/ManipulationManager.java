package org.commonjava.maven.ext.manip;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.impl.Manipulator;
import org.commonjava.maven.ext.manip.out.PomModifier;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

/**
 * Coordinates manipulation of the POMs in a build, by providing methods to read the project set from files ahead of the build proper (using 
 * {@link ProjectBuilder}), then other methods to coordinate all potential {@link Manipulator} implementations (along with the {@link PomModifier} 
 * raw-model reader/rewriter).
 * 
 * @author jdcasey
 */
@Component( role = ManipulationManager.class )
public class ManipulationManager
{

    @Requirement
    protected Logger logger;

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement( role = Manipulator.class )
    private Map<String, Manipulator> manipulators;

    /**
     * Scan the projects implied by the given POM file for modifications, and save the state in the session for later rewriting to apply it.
     */
    public void scan( final File pom, final ManipulationSession session )
        throws ManipulationException
    {
        final List<MavenProject> projects = buildProjects( pom, session );
        session.setProjectInstances( projects );

        for ( final Map.Entry<String, Manipulator> entry : manipulators.entrySet() )
        {
            entry.getValue()
                 .scan( projects, session );
        }
    }

    /**
     * Uses {@link ProjectBuilder} with a custom {@link ProjectBuildingRequest} to read the unmodified {@link MavenProject} instances from disk.
     */
    protected List<MavenProject> buildProjects( final File rootPom, final ManipulationSession session )
        throws ManipulationException
    {
        final ProjectBuildingRequest pbr = session.getProjectBuildingRequest();
        final boolean recurse = session.isRecursive();

        final DefaultProjectBuildingRequest wrapper = new DefaultProjectBuildingRequest( pbr );
        wrapper.setRepositorySession( session.getRepositorySystemSession() );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        // We have no POM file.
        if ( rootPom == null )
        {
            return Collections.emptyList();
        }
        else
        {
            final List<File> files = Arrays.asList( rootPom );
            List<ProjectBuildingResult> results;
            try
            {
                results = projectBuilder.build( files, recurse, wrapper );
            }
            catch ( final ProjectBuildingException e )
            {
                throw new ManipulationException( "Failed to build MavenProject instances: %s", e, e.getMessage() );
            }

            for ( final ProjectBuildingResult result : results )
            {
                projects.add( result.getProject() );
            }
        }

        return projects;
    }

    /**
     * After projects are scanned for modifications, apply any modifications and rewrite POMs as needed. This method performs the following:
     * <ul>
     *   <li>read the raw models (uninherited, with only a bare minimum interpolation) from disk to escape any interpretation happening during project-building</li>
     *   <li>apply any manipulations from the previous {@link ManipulationManager#scan(File, ManipulationSession)} call</li>
     *   <li>rewrite any POMs that were changed</li>
     * </ul>
     */
    public void applyManipulations( final List<MavenProject> projects, final ManipulationSession session )
        throws ManipulationException
    {
        PomModifier.readModelsForManipulation( projects, session );

        boolean changed = false;
        for ( final Map.Entry<String, Manipulator> entry : manipulators.entrySet() )
        {
            changed = entry.getValue()
                           .applyChanges( projects, session ) || changed;
        }

        if ( changed )
        {
            logger.info( "REWRITE CHANGED: " + projects );
            PomModifier.rewriteChangedPOMs( projects, session );
        }
        else
        {
            logger.info( "NO CHANGES." );
        }
    }

    /**
     * Initialize {@link ManipulationSession} using the given {@link MavenSession} instance, along with any state managed by the individual
     * {@link Manipulator} components.
     */
    public void init( final MavenSession mavenSession, final ManipulationSession session )
        throws ManipulationException
    {
        session.setMavenSession( mavenSession );

        for ( final Map.Entry<String, Manipulator> entry : manipulators.entrySet() )
        {
            entry.getValue()
                 .init( session );
        }
    }

}
