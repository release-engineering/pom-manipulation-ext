package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.IdUtils.ga;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.Repository;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.RepoReportingState;
import org.commonjava.maven.ext.manip.state.State;

/**
 * {@link Manipulator} implementation that can remove Reporting and Repository sections from a project's pom file.
 * Configuration is stored in a {@link RepoReportingState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "repo-reporting-removal" )
public class RepoAndReportingRemovalManipulator
    implements Manipulator
{

    @Requirement
    protected Logger logger;

    protected RepoAndReportingRemovalManipulator()
    {
    }

    public RepoAndReportingRemovalManipulator( final Logger logger )
    {
        this.logger = logger;
    }

    /**
     * No prescanning required for Repository and Reporting Removal.
     */
    @Override
    public void scan( final List<MavenProject> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link RepoReportingState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link RepoAndReportingRemovalManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new RepoReportingState( userProps ) );
    }

    /**
     * Apply the reporting and repository removal changes to the list of {@link MavenProject}'s given.
     * This happens near the end of the Maven session-bootstrapping sequence, before the projects are
     * discovered/read by the main Maven build initialization.
     */
    @Override
    public boolean applyChanges( final List<MavenProject> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final State state = session.getState( RepoReportingState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.info( "Version Manipulator: Nothing to do!" );
            return false;
        }

        final Map<String, Model> manipulatedModels = session.getManipulatedModels();

        boolean changed = false;
        for ( final MavenProject project : projects )
        {
            final String ga = ga( project );
            logger.info( "Applying changes to: " + ga );
            final Model model = manipulatedModels.get( ga );

            if ( model.getRepositories() != null && !model.getRepositories().isEmpty() )
            {
                model.setRepositories( Collections.<Repository>emptyList() );
                changed = true;
            }

            if ( model.getPluginRepositories() != null && !model.getPluginRepositories().isEmpty() )
            {
                model.setPluginRepositories( Collections.<Repository>emptyList() );
                changed = true;
            }

            if ( model.getReporting() != null )
            {
                model.setReporting( null );
                changed = true;
            }
        }

        return changed;
    }
}
