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
            throw new Error( "Modification failed during project pre-scanning phase: " + e.getMessage(), e );
        }

        super.onEvent( event );
    }

}
