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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.apache.maven.model.building.ModelBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.slf4j.Logger;
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
    private static final String MARKER_PATH = "target";

    private static final String MARKER_FILE =  MARKER_PATH + File.separatorChar + "pom-manip-ext-marker.txt";

    private static final String REQUIRE_EXTENSION = "manipulation.required";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private ManipulationManager manipulationManager;

    @Requirement
    private ModelBuilder modelBuilder;

    @Requirement
    private ManipulationSession session;

    @Override
    public void onEvent( final Object event )
        throws Exception
    {
        boolean required = false;

        try
        {
             if ( event instanceof ExecutionEvent )
            {
                final ExecutionEvent ee = (ExecutionEvent) event;

                required = Boolean.parseBoolean( ee.getSession()
                                                   .getRequest()
                                                   .getUserProperties()
                                                   .getProperty( REQUIRE_EXTENSION, "false" ) );

                final ExecutionEvent.Type type = ee.getType();
                if ( type == Type.ProjectDiscoveryStarted )
                {
                    if ( ee.getSession() != null )
                    {
                        if ( ee.getSession()
                               .getRequest()
                               .getLoggingLevel() == 0 )
                        {
                            final ch.qos.logback.classic.Logger root =
                                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME );
                            root.setLevel( Level.DEBUG );
                        }

                        manipulationManager.init( ee.getSession(), session );
                    }

                    if ( !session.isEnabled() )
                    {
                        logger.info( "Manipulation engine disabled{}",
                                     ( session.getExecutionRoot() == null ? ". No project found."
                                                     : " via command-line option" ) );

                        super.onEvent( event );
                        return;
                    }
                    else if ( new File ( session.getExecutionRoot().getParentFile(), MARKER_FILE).exists() )
                    {
                        logger.info( "Skipping manipulation as previous execution found." );

                        super.onEvent( event );
                        return;
                    }

                    manipulationManager.scan( session.getExecutionRoot(), session );

                    final List<Project> projects = session.getProjects();

                    for ( final Project project : projects )
                    {
                        logger.debug( "Got " + project + " (POM: " + project.getPom() + ")" );
                    }

                    // Create a marker file if we made some changes to prevent duplicate runs.
                    if ( ! manipulationManager.applyManipulations( projects, session ).isEmpty() )
                    {
                        try
                        {
                            new File (session.getExecutionRoot().getParentFile(), MARKER_PATH).mkdirs();
                            new File (session.getExecutionRoot().getParentFile(), MARKER_FILE).createNewFile();
                        }
                        catch ( IOException e )
                        {
                            throw new ManipulationException ("Marker file creation failed", e);
                        }
                    }
                }
            }
        }
        catch ( final ManipulationException e )
        {
            logger.error( "Extension failure", e );
            if ( required )
            {
                throw e;
            }
            else
            {
                session.setError( e );
            }
        }

        super.onEvent( event );
    }
}
