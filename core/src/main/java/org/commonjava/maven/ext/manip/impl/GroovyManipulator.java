/*
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

import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.GroovyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * {@link Manipulator} implementation that can resolve a remote groovy file and execute it on executionRoot. Configuration
 * is stored in a {@link org.commonjava.maven.ext.manip.state.GroovyState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "groovy-injection" )
public class GroovyManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected ModelIO modelBuilder;

    /**
     * No prescanning required for Profile injection.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link GroovyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link GroovyManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new GroovyState( userProps ) );
    }

    /**
     * Apply the groovy script changes to the top level pom.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final GroovyState state = session.getState( GroovyState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();
        final List<ArtifactRef> scripts = state.getGroovyScripts();

        for (ArtifactRef ar : scripts)
        {
            logger.info ("Attempting to read GAV {} with classifier {} and type {} ",
                         ar.asProjectVersionRef(), ar.getClassifier(), ar.getType());

            final File groovyScript = modelBuilder.resolveRawFile( ar );

            GroovyShell shell = new GroovyShell( );
            Script script;

            for ( final Project project : projects )
            {
                if ( project.isExecutionRoot() )
                {
                    logger.info ("Executing {} on {}", groovyScript, project);

                    try
                    {
                        script = shell.parse( groovyScript );

                        script.invokeMethod( "setValues", new Object[] { session.getUserProperties(), projects, project } );
                    }
                    catch (MissingMethodException e)
                    {
                        try
                        {
                            logger.debug ( "Failure when injecting into script {} ", FileUtils.readFileToString( groovyScript ) );
                        }
                        catch ( IOException e1 )
                        {
                            logger.debug ("Unable to read script file {} for debugging! {} ", groovyScript, e1);
                        }
                        throw new ManipulationException( "Unable to inject values into base script", e );
                    }
                    catch (CompilationFailedException e)
                    {
                        try
                        {
                            logger.debug ( "Failure when parsing script {} ", FileUtils.readFileToString( groovyScript ) );
                        }
                        catch ( IOException e1 )
                        {
                            logger.debug ("Unable to read script file {} for debugging! {} ", groovyScript, e1);
                        }
                        throw new ManipulationException( "Unable to parse script", e );
                    }
                    catch ( IOException e )
                    {
                        throw new ManipulationException( "Unable to parse script", e );
                    }
                    try
                    {
                        script.run();
                    }
                    catch ( Exception e )
                    {
                        throw new ManipulationException( "Unable to parse script", e );
                    }

                    changed.add( project );
                }
            }
        }
        return changed;
    }


    // Groovy script manipulation should run last.
    @Override
    public int getExecutionIndex()
    {
        return 99;
    }
}
