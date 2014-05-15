package org.commonjava.maven.ext.manip;

import java.util.List;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.resolver.EffectiveModelBuilder;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.sonatype.aether.impl.ArtifactResolver;

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

    @Requirement
    private ArtifactResolver resolver;

    @Requirement
    private ModelBuilder modelBuilder;

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

                        try
                        {
                            EffectiveModelBuilder.init( logger, ee.getSession(), resolver, modelBuilder );
                        }
                        catch ( ComponentLookupException e )
                        {
                            logger.error( "EffectiveModelBuilder init could not look up plexus component: " + e );
                        }
                        catch ( PlexusContainerException e )
                        {
                            logger.error( "EffectiveModelBuilder init produced a plexus container error: " + e );
                        }

                        logger.info( "Pre-scanning projects to calculate changes..." );

                        final MavenExecutionRequest req = session.getRequest();

                        manipulationManager.scan( req.getPom(), session );

                        //                        logger.info( "Rewriting projects with manipulation changes:\n\n  " + join( session.getVersioningChanges()
                        //                                                                                                          .entrySet()
                        //                                                                                                          .iterator(), "\n  " ) + "\n\n" );

                        final List<MavenProject> projects = session.getProjectInstances();

                        for ( final MavenProject project : projects )
                        {
                            logger.debug( "Got " + project + " (POM: " + project.getOriginalModel()
                                                                               .getPomFile() + ")" );
                        }

                        manipulationManager.applyManipulations( projects, session );

                        break;
                    }
                    //                    case SessionStarted:
                    //                    {
                    //                        break;
                    //                    }
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
            throw new Error( "Modification failed during project pre-scanning phase: " + e.getMessage(), e );
        }

        super.onEvent( event );
    }

}
