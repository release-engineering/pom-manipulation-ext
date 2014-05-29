/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.ext.manip;

import java.util.List;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

/**
 * Implements hooks necessary to apply modifications in the Maven bootstrap, before the build starts.
 * @author jdcasey
 */
@Component( role = EventSpy.class, hint = "manipulation" )
public class ManipulatingEventSpy
    extends AbstractEventSpy
{
    @Requirement
    private Logger logger;

    @Requirement
    private ManipulationManager manipulationManager;

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

                        final MavenExecutionRequest req = session.getRequest();

                        manipulationManager.scan( req.getPom(), session );

                        final List<Project> projects = session.getProjects();

                        for ( final Project project : projects )
                        {
                            logger.debug( "Got " + project + " (POM: " + project.getPom() + ")" );
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
                    //                        baseLogger.info( "ExecutionEvent TYPE: " + ee.getType() );
                    //                    }
                }
                if ( ee.getType() == Type.ProjectDiscoveryStarted )
                {
                }
            }
        }
        catch ( final ManipulationException e )
        {
            logger.error( "Extension failure", e );
            session.setError( e );
        }

        super.onEvent( event );
    }

    @Override
    public void init( final Context context )
        throws Exception
    {
        if ( logger.isDebugEnabled() )
        {
            final ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
            root.setLevel( Level.DEBUG );
        }
    }

}
