/**
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.manip;

import ch.qos.logback.classic.Level;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionEvent.Type;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Implements hooks necessary to apply modificationprojectBs in the Maven bootstrap, before the build starts.
 * @author jdcasey
 */
@Component( role = EventSpy.class, hint = "manipulation" )
public class ManipulatingEventSpy
    extends AbstractEventSpy
{

    private static final String REQUIRE_EXTENSION = "manipulation.required";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private ManipulationManager manipulationManager;

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
                        session.setMavenSession( ee.getSession() );
                        manipulationManager.init( session );
                    }
                    else
                    {
                        logger.error( "Null session ; unable to continue" );
                        return;
                    }

                    if ( !session.isEnabled() )
                    {
                        logger.info( "Manipulation engine disabled via command-line option" );
                        return;
                    }
                    else if ( ee.getSession().getRequest().getPom() == null )
                    {
                        logger.info( "Manipulation engine disabled. No project found." );
                        return;
                    }
                    else if ( new File ( ee.getSession().getRequest().getPom().getParentFile(), ManipulationManager.MARKER_FILE).exists() )
                    {
                        logger.info( "Skipping manipulation as previous execution found." );
                        return;
                    }

                    manipulationManager.scanAndApply ( session );
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
        // Catch any runtime exceptions and mark them to fail the build as well.
        catch ( final RuntimeException e )
        {
            logger.error( "Extension failure", e );
            if ( required )
            {
                throw e;
            }
            else
            {
                session.setError( new ManipulationException( "Caught runtime exception", e ) );
            }
        }
        finally
        {
            super.onEvent( event );
        }
    }
}
