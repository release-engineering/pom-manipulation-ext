package org.commonjava.maven.ext.manip;

import java.util.List;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

/**
 * Implements hooks necessary to apply modifications in the Maven bootstrap, before the build starts.
 * @author jdcasey
 */
@Component( role = EventSpy.class, hint = "versioning" )
public class ManipulatingEventSpy
    extends AbstractEventSpy
{

    @Requirement
    private Logger logger;

    @Requirement
    private ManipulationManager manipulationManager;

    // FIXME: This was a classic getInstance() singleton...injection MAY not work here.
    @Requirement
    private ManipulationSession session;

    @SuppressWarnings( "incomplete-switch" )
    @Override
    public void onEvent( final Object event )
        throws Exception
    {
        try
        {
            //            if ( event instanceof MavenExecutionRequest )
            //            {
            //                session.setRequest( (MavenExecutionRequest) event );
            //            }

            if ( event instanceof ExecutionEvent )
            {
                final ExecutionEvent ee = (ExecutionEvent) event;

                final ExecutionEvent.Type type = ee.getType();
                switch ( type )
                {
                    case ProjectDiscoveryStarted:
                    {
                        if ( ee.getSession() != null )
                        {
                            manipulationManager.init( ee.getSession(), session );
                        }

                        if ( !session.isEnabled() )
                        {
                            logger.info( "Manipulation engine disabled." );
                            super.onEvent( event );
                            return;
                        }

                        logger.info( "Pre-scanning projects to calculate versioning changes..." );
                        final MavenExecutionRequest req = session.getRequest();

                        manipulationManager.scan( req.getPom(), session );
                        break;
                    }
                    case SessionStarted:
                    {
                        //                        logger.info( "Rewriting projects with manipulation changes:\n\n  " + join( session.getVersioningChanges()
                        //                                                                                                          .entrySet()
                        //                                                                                                          .iterator(), "\n  " ) + "\n\n" );

                        final List<MavenProject> projects = ee.getSession()
                                                              .getProjects();

                        for ( final MavenProject project : projects )
                        {
                            logger.info( "Got " + project + " (POM: " + project.getOriginalModel()
                                                                               .getPomFile() + ")" );
                        }

                        manipulationManager.applyManipulations( projects, session );

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
        catch ( final ManipulationException e )
        {
            throw new Error( "Versioning modification failed during project pre-scanning phase: " + e.getMessage(), e );
        }

        super.onEvent( event );
    }

}
