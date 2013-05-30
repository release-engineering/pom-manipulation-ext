package org.commonjava.maven.ext.versioning;

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
            //            logger.info( "EVENT: " + event.getClass()
            //                                          .getName() + " :: " + event );
            //
            if ( event instanceof MavenExecutionRequest )
            {
                final VersioningSession session = getSession();
                session.setRequest( (MavenExecutionRequest) event );
            }

            if ( event instanceof ExecutionEvent )
            {
                final ExecutionEvent ee = (ExecutionEvent) event;

                final ExecutionEvent.Type type = ee.getType();
                switch ( type )
                {
                    case ProjectDiscoveryStarted:
                    {
                        logger.info( "Pre-scanning projects to calculate versioning changes..." );
                        final VersioningSession session = getSession();
                        session.setVersioningChanges( scanner.scanVersioningChanges( session.getRequest(),
                                                                                     ee.getSession() ) );
                        break;
                    }
                    case SessionStarted:
                    {
                        logger.info( "Rewriting projects with versioning changes..." );
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

    private VersioningSession getSession()
    {
        return VersioningSession.getInstance();
    }

}
