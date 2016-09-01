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
package org.commonjava.maven.ext.manip.impl;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPathException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.JSONIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.JSONState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * {@link Manipulator} implementation that can modify JSON files. Configuration
 * is stored in a {@link JSONState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "json-manipulator" )
public class JSONManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private JSONIO jsonIO;

    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link JSONState} state holder in the {@link ManipulationSession}. This state holder detects
     * configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link JSONManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
                    throws ManipulationException
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new JSONState( userProps ) );
    }

    /**
     * Apply the json changes to the specified file(s).
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final JSONState state = session.getState( JSONState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();
        final List<JSONState.JSONOperation> scripts = state.getJSONOperations();

        for ( final Project project : projects )
        {
            if ( project.isExecutionRoot() )
            {
                for ( JSONState.JSONOperation operation : scripts )
                {
                    File target = new File( project.getPom().getParentFile(), operation.getFile() );

                    logger.info( "Attempting to start JSON update to file {} with xpath {} and replacement '{}' ",
                                 target, operation.getXPath(), operation.getUpdate() );

                    internalApplyChanges (target, operation);

                    changed.add( project );
                }

                break;
            }
        }
        return changed;
    }

    // Package accessible so tests can use it.
    void internalApplyChanges( File target, JSONState.JSONOperation operation ) throws ManipulationException
    {
        DocumentContext dc = null;
        try
        {
            if ( !target.exists() )
            {
                logger.error( "Unable to locate JSON file {} ", target );
                throw new ManipulationException( "Unable to locate JSON file " + target );
            }

            dc = jsonIO.parseJSON( target );

            List o = dc.read( operation.getXPath() );
            if ( o.size() == 0 )
            {
                logger.error( "XPath {} did not find any expressions within {} ", operation.getXPath(),
                              operation.getFile() );
                throw new ManipulationException( "XPath did not resolve to a valid value" );
            }

            if ( isEmpty( operation.getUpdate() ) )
            {
                // Delete
                logger.info( "Deleting {} on {}", operation.getXPath(), dc.toString() );
                dc.delete( operation.getXPath() );
            }
            else
            {
                // Update
                logger.info( "Updating {} on {}", operation.getXPath(), dc.toString() );
                dc.set( operation.getXPath(), operation.getUpdate() );
            }

            jsonIO.writeJSON( target, dc.jsonString() );
        }
        catch ( JsonPathException e )
        {
            logger.error( "Caught JSON exception processing file {}, document context {} ", target, dc, e );
            throw new ManipulationException( "Caught JsonPath", e );
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 98;
    }
}
