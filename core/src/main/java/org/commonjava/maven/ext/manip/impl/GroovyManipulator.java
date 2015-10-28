/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.commonjava.maven.ext.manip.impl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
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

        final Set<Project> changed = new HashSet<Project>();
        final List<ArtifactRef> scripts = state.getGroovyScripts();

        for (ArtifactRef ar : scripts)
        {
            logger.info ("Attempting to read GAV {} with classifier {} and type {} ",
                         ar.asProjectVersionRef(), ar.getClassifier(), ar.getType());

            final File groovyScript = modelBuilder.resolveRawFile( ar );

            Binding binding = new Binding( );
            GroovyShell shell = new GroovyShell( binding );
            Script script = null;

            for ( final Project project : projects )
            {
                if ( project.isExecutionRoot() )
                {
                    binding.setProperty( "basedir", project.getPom().getParentFile().toString() );
                    binding.setProperty( "name", project.getKey() );
                    binding.setProperty( "project", project );
                    binding.setProperty( "projects", projects );

                    logger.info ("Executing {} on {} with binding {} ", groovyScript, project, binding.getVariables());

                    try
                    {
                        script = shell.parse( groovyScript );
                    }
                    catch (CompilationFailedException e)
                    {
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


    @Override
    public int getExecutionIndex()
    {
        return 90;
    }
}
