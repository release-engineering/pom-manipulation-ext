package org.commonjava.maven.ext.versioning;

import static org.codehaus.plexus.util.StringUtils.join;

import java.io.IOException;
import java.util.List;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component( role = EventSpy.class, hint = "versioning" )
public class VersioningEventSpy
    extends AbstractEventSpy
{

    @Requirement
    private Logger logger;

    @Requirement
    private ProjectVersioningScanner scanner;

    @Requirement
    private VersioningModifier modder;

    @SuppressWarnings( "incomplete-switch" )
    @Override
    public void onEvent( final Object event )
        throws Exception
    {
        try
        {
            if ( event instanceof MavenExecutionRequest )
            {
                final VersioningSession session = VersioningSession.getInstance();
                session.setRequest( (MavenExecutionRequest) event );
            }

            if ( event instanceof ExecutionEvent )
            {
                final VersioningSession session = VersioningSession.getInstance();
                if ( !session.isEnabled() )
                {
                    return;
                }

                final ExecutionEvent ee = (ExecutionEvent) event;

                if ( ee.getSession() != null )
                {
                    session.setRepositorySystemSession( ee.getSession()
                                                          .getRepositorySession() );
                }

                final ExecutionEvent.Type type = ee.getType();
                switch ( type )
                {
                    case ProjectDiscoveryStarted:
                    {
                        logger.info( "Pre-scanning projects to calculate versioning changes..." );
                        final MavenExecutionRequest req = session.getRequest();

                        session.setVersioningChanges( scanner.scanVersioningChanges( req.getPom(), ee.getSession(),
                                                                                     req.getProjectBuildingRequest(),
                                                                                     req.isRecursive() ) );
                        break;
                    }
                    case SessionStarted:
                    {
                        logger.info( "Rewriting projects with versioning changes:\n\n  "
                            + join( session.getVersioningChanges()
                                           .entrySet()
                                           .iterator(), "\n  " ) + "\n\n" );

                        final List<MavenProject> projects = ee.getSession()
                                                              .getProjects();

                        for ( final MavenProject project : projects )
                        {
                            logger.info( "Got " + project + " (POM: " + project.getOriginalModel()
                                                                               .getPomFile() + ")" );
                        }

                        modder.rewriteChangedPOMs( projects );

                        break;
                    }
                    //                    default:
                    //                    {
                    //                        logger.info( "ExecutionEvent TYPE: " + ee.getType() );
                    //                    }
                }
                if ( ee.getType() == Type.ProjectDiscoveryStarted )
                {
                }
            }
        }
        catch ( final VersionModifierException e )
        {
            throw new Error( "Versioning modification failed during project pre-scanning phase: " + e.getMessage(), e );
        }
        catch ( final ProjectBuildingException e )
        {
            throw new Error( "Versioning modification failed during project pre-scanning phase: " + e.getMessage(), e );
        }
        catch ( final IOException e )
        {
            throw new Error( "Versioning modification failed during POM rewriting phase: " + e.getMessage(), e );
        }

        super.onEvent( event );
    }

}
