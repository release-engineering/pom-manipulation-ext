package org.commonjava.maven.ext.versioning;

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

@Component( role = ProjectVersioningScanner.class )
public class ProjectVersioningScanner
{

    @Requirement
    private VersioningModifier modder;

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private Logger logger;

    @Requirement
    private VersionCalculator calculator;

    public Map<String, String> scanVersioningChanges( final File rootPom, final MavenSession session,
                                                      final ProjectBuildingRequest pbr, final boolean recurse )
        throws ProjectBuildingException, VersionModifierException
    {
        final List<MavenProject> projects = scan( rootPom, session, pbr, recurse );

        final VersioningSession vSession = VersioningSession.getInstance();
        if ( !vSession.isEnabled() )
        {
            logger.info( "Versioning Extension: Nothing to do!" );
            return Collections.emptyMap();
        }

        logger.info( "Versioning Extension: Calculating the necessary versioning changes." );
        final Map<String, String> versionsByGA = calculator.calculateVersioningChanges( projects );

        return versionsByGA;
    }

    protected List<MavenProject> scan( final File rootPom, final MavenSession session,
                                       final ProjectBuildingRequest pbr, final boolean recurse )
        throws ProjectBuildingException
    {
        final DefaultProjectBuildingRequest wrapper = new DefaultProjectBuildingRequest( pbr );
        wrapper.setRepositorySession( session.getRepositorySession() );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        // We have no POM file.
        if ( rootPom == null )
        {
            return Collections.emptyList();
        }
        else
        {
            final List<File> files = Arrays.asList( rootPom );
            collectProjects( projects, files, wrapper, recurse );
        }
        return projects;
    }

    protected void collectProjects( final List<MavenProject> projects, final List<File> files,
                                    final ProjectBuildingRequest projectBuildingRequest, final boolean recurse )
        throws ProjectBuildingException
    {
        final List<ProjectBuildingResult> results = projectBuilder.build( files, recurse, projectBuildingRequest );

        for ( final ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );
        }
    }

}
