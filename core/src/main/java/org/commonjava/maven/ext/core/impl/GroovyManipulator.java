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
package org.commonjava.maven.ext.core.impl;

import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;
import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.groovy.BaseScript;
import org.commonjava.maven.ext.core.state.GroovyState;
import org.commonjava.maven.ext.io.FileIO;
import org.commonjava.maven.ext.io.ModelIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * {@link Manipulator} implementation that can resolve a remote groovy file and execute it on executionRoot. Configuration
 * is stored in a {@link org.commonjava.maven.ext.core.state.GroovyState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Named("groovy-injection")
@Singleton
public class GroovyManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected ModelIO modelBuilder;

    private FileIO fileIO;

    private ManipulationSession session;

    private int executionIndex = 99;

    @Inject
    public GroovyManipulator(ModelIO modelIO, FileIO fileIO)
    {
        this.modelBuilder = modelIO;
        this.fileIO = fileIO;
    }
    /**
     * Initialize the {@link GroovyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        GroovyState gs = new GroovyState( session.getUserProperties() );
        this.session = session;
        this.executionIndex = gs.getExecutionIndex();
        session.setState( gs );
    }

    /**
     * Apply the groovy script changes to the top level pom.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
        throws ManipulationException
    {
        final GroovyState state = session.getState( GroovyState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();

        for ( File groovyScript : parseGroovyScripts( state.getGroovyScripts() ))
        {
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

                        if ( script instanceof BaseScript )
                        {
                            ((BaseScript)script).setValues(session.getUserProperties(), projects, project);
                        }
                        else
                        {
                            throw new ManipulationException( "Cannot cast " + groovyScript + " to a BaseScript to set values." );
                        }
                    }
                    catch (MissingMethodException e)
                    {
                        try
                        {
                            logger.debug ( "Failure when injecting into script {} ", FileUtils.readFileToString( groovyScript ), e );
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
                            logger.debug ( "Failure when parsing script {} ", FileUtils.readFileToString( groovyScript ), e );
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
        return executionIndex;
    }

    /**
     * Splits the value on ',', then wraps each value in {@link SimpleArtifactRef#parse(String)} and prints a warning / skips in the event of a
     * parsing error. Returns null if the input value is null.
     * @param value a comma separated list of GAVTC to parse
     * @return a collection of parsed ArtifactRef.
     */
    public List<File> parseGroovyScripts( final String value ) throws ManipulationException
    {
        if ( isEmpty( value ) )
        {
            return Collections.emptyList();
        }
        else
        {
            final List<File> result = new ArrayList<>();

            logger.debug( "Processing groovy scripts {} ", value );
            try
            {
                final String[] scripts = value.split( "," );
                for ( final String script : scripts )
                {
                    File found;
                    if ( script.startsWith( "http" ) )
                    {
                        logger.info( "Attempting to read URL {} ", script );
                        found = fileIO.resolveURL( new URL( script ) );
                    }
                    else
                    {
                        final ArtifactRef ar = SimpleArtifactRef.parse( script );
                        logger.info( "Attempting to read GAV {} with classifier {} and type {} ", ar.asProjectVersionRef(), ar.getClassifier(), ar.getType() );
                        found = modelBuilder.resolveRawFile( ar );
                    }
                    result.add( found );
                }
            }
            catch ( IOException e )
            {
                throw new ManipulationException( "Unable to parse groovyScripts", e );
            }
            return result;
        }
    }
}
