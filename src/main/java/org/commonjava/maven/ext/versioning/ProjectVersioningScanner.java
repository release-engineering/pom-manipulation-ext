package org.commonjava.maven.ext.versioning;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.DefaultMaven;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.UrlModelSource;
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

    public Map<String, String> scanVersioningChanges( final MavenExecutionRequest request, final MavenSession session )
        throws ProjectBuildingException
    {
        final List<MavenProject> projects = scan( request, session );

        final VersionCalculator calculator = new VersionCalculator( session.getUserProperties() );
        if ( !calculator.isEnabled() )
        {
            logger.info( "Versioning Extension: Nothing to do!" );
            return Collections.emptyMap();
        }

        logger.info( "Versioning Extension: Applying version suffix: " + calculator.getSuffix() );
        final Map<String, String> versionsByGA = calculator.calculateVersioningChanges( projects );

        return versionsByGA;
    }

    protected List<MavenProject> scan( final MavenExecutionRequest request, final MavenSession session )
        throws ProjectBuildingException
    {
        request.getProjectBuildingRequest()
               .setRepositorySession( session.getRepositorySession() );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        // We have no POM file.
        //
        if ( request.getPom() == null )
        {
            final ModelSource modelSource =
                new UrlModelSource( DefaultMaven.class.getResource( "project/standalone.xml" ) );
            final MavenProject project = projectBuilder.build( modelSource, request.getProjectBuildingRequest() )
                                                       .getProject();

            project.setExecutionRoot( true );
            projects.add( project );
            request.setProjectPresent( false );
        }
        else
        {
            final List<File> files = Arrays.asList( request.getPom()
                                                           .getAbsoluteFile() );
            collectProjects( projects, files, request );
        }
        return projects;
    }

    protected void collectProjects( final List<MavenProject> projects, final List<File> files,
                                    final MavenExecutionRequest request )
        throws ProjectBuildingException
    {
        final ProjectBuildingRequest projectBuildingRequest = request.getProjectBuildingRequest();

        final List<ProjectBuildingResult> results =
            projectBuilder.build( files, request.isRecursive(), projectBuildingRequest );

        for ( final ProjectBuildingResult result : results )
        {
            projects.add( result.getProject() );
        }
    }

}
