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
package org.commonjava.maven.ext.core;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.GAV;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.impl.Manipulator;
import org.commonjava.maven.ext.core.state.CommonState;
import org.commonjava.maven.ext.core.state.State;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.core.util.ManipulatorPriorityComparator;
import org.commonjava.maven.ext.io.PomIO;
import org.commonjava.maven.ext.io.resolver.ExtensionInfrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates manipulation of the POMs in a build, by providing methods to read the project set from files ahead of the build proper (using
 * {@link ProjectBuilder}), then other methods to coordinate all potential {@link Manipulator} implementations (along with the {@link PomIO}
 * raw-model reader/rewriter).
 * <p>
 * Sequence of calls:
 * <ol>
 *   <li>{@link #init(ManipulationSession)}</li>
 *   <li>{@link #applyManipulations(List)}</li>
 * </ol>
 * 
 * @author jdcasey
 */
@Component( role = ManipulationManager.class )
public class ManipulationManager
{
    private static final String MARKER_PATH = "target";

    public static final String MARKER_FILE =  MARKER_PATH + File.separatorChar + "pom-manip-ext-marker.txt";

    public static final String RESULT_FILE = MARKER_PATH + File.separatorChar + "pom-manip-ext-result.json";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement( role = Manipulator.class )
    private Map<String, Manipulator> manipulators;

    @Requirement( role = ExtensionInfrastructure.class )
    private Map<String, ExtensionInfrastructure> infrastructure;

    @Requirement
    private PomIO pomIO;

    /**
     * Determined from {@link Manipulator#getExecutionIndex()} comparisons during {@link #init(ManipulationSession)}.
     */
    private List<Manipulator> orderedManipulators;

    /**
     * Initialize {@link ManipulationSession} using the given {@link MavenSession} instance, along with any state managed by the individual
     * {@link Manipulator} components.
     *
     * @param session the container session for manipulation.
     * @throws ManipulationException if an error occurs.
     */
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Initialising ManipulationManager with user properties {}", session.getUserProperties() );

        for ( final ExtensionInfrastructure infra : infrastructure.values() )
        {
            infra.init( session.getTargetDir(), session.getRemoteRepositories(), session.getLocalRepository(),
                        session.getSettings(), session.getActiveProfiles() );
        }

        orderedManipulators = new ArrayList<>( manipulators.values() );
        // The RESTState depends upon the VersionState being initialised. Therefore initialise in reverse order
        // and do a final sort to run in the correct order. See the Manipulator interface for detailed discussion
        // on ordered.
        Collections.sort( orderedManipulators, Collections.reverseOrder( new ManipulatorPriorityComparator() ) );

        for ( final Manipulator manipulator : orderedManipulators )
        {
            logger.debug( "Initialising manipulator " + manipulator.getClass()
                                                                   .getSimpleName() );
            manipulator.init( session );
        }
        Collections.sort( orderedManipulators, new ManipulatorPriorityComparator() );

        // Now init the common state
        session.setState( new CommonState( session.getUserProperties()) );

    }

    /**
     * Encapsulates {@link #applyManipulations(List)}
     *
     * @param session the container session for manipulation.
     * @throws ManipulationException if an error occurs.
     */
    public void scanAndApply( final ManipulationSession session )
                    throws ManipulationException
    {
        final List<Project> projects = pomIO.parseProject( session.getPom() );

        session.setProjects( projects );

        for ( final Project project : projects )
        {
            logger.debug( "Got " + project + " (POM: " + project.getPom() + ")" );
        }

        Set<Project> changed = applyManipulations( projects );

        // Create a marker file if we made some changes to prevent duplicate runs.
        if ( !changed.isEmpty() )
        {
            logger.info( "Maven-Manipulation-Extension: Rewrite changed: " + projects );

            GAV gav = pomIO.rewritePOMs( changed );

            try
            {
                final VersioningState state = session.getState( VersioningState.class );
                state.setExecutionRootModified( gav );

                new File( session.getTargetDir().getParentFile(),
                          ManipulationManager.MARKER_PATH ).mkdirs();

                new File( session.getTargetDir().getParentFile(),
                          ManipulationManager.MARKER_FILE ).createNewFile();

                try (FileWriter writer = new FileWriter( new File ( session.getTargetDir().getParentFile(), RESULT_FILE ) ) )
                {
                    writer.write( collectResults( session ) );
                }
            }
            catch ( IOException e )
            {
                logger.error( "Unable to create marker or result file", e );
                throw new ManipulationException( "Marker/result file creation failed", e );
            }
        }

        // Ensure shutdown of GalleyInfrastructure Executor Service
        for (ExtensionInfrastructure e : infrastructure.values())
        {
            e.finish();
        }
        logger.info( "Maven-Manipulation-Extension: Finished." );
    }

    /**
     * After projects are scanned for modifications, apply any modifications and rewrite POMs as needed. This method performs the following:
     * <ul>
     *   <li>read the raw models (uninherited, with only a bare minimum interpolation) from disk to escape any interpretation happening during project-building</li>
     *   <li>apply any manipulations
     *   <li>rewrite any POMs that were changed</li>
     * </ul>
     *
     * @param projects the list of Projects to apply the changes to.
     * @return collection of the changed projects.
     * @throws ManipulationException if an error occurs.
     */
    private Set<Project> applyManipulations( final List<Project> projects )
        throws ManipulationException
    {
        final Set<Project> changed = new HashSet<>();
        for ( final Manipulator manipulator : orderedManipulators )
        {
            final Set<Project> mChanged = manipulator.applyChanges( projects );

            if ( mChanged != null )
            {
                changed.addAll( mChanged );
            }
        }

        if ( changed.isEmpty() )
        {
            logger.info( "Maven-Manipulation-Extension: No changes." );
        }

        return changed;
    }

    /**
     * After the modifications are applied, it may be useful for manipulators
     * to provide caller with a structured, computer-readable output or summary of the changes.
     * This is done in the form of a JSON document stored in the root target
     * directory.
     * The output data are generated from the fields of the state objects,
     * which must be actively marked by {@link JsonProperty} annotation
     * to be processed.
     * The result is a map from short state class names
     * to the result of the state serialization.
     * Keys with empty values are excluded.
     *
     * @param session the container session for manipulation.
     * @throws ManipulationException if an error occurs.
     */
    private String collectResults( final ManipulationSession session )
                    throws ManipulationException, JsonProcessingException
    {
        final ObjectMapper MAPPER = new ObjectMapper();
        VisibilityChecker<?> vc = MAPPER.getSerializationConfig()
                                        .getDefaultVisibilityChecker()
                                        .withCreatorVisibility( JsonAutoDetect.Visibility.NONE )
                                        .withFieldVisibility( JsonAutoDetect.Visibility.NONE )
                                        .withGetterVisibility( JsonAutoDetect.Visibility.NONE )
                                        .withIsGetterVisibility( JsonAutoDetect.Visibility.NONE )
                                        .withSetterVisibility( JsonAutoDetect.Visibility.NONE );
        MAPPER.setVisibility( vc );
        MAPPER.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
        ObjectNode root = MAPPER.createObjectNode();
        for ( final Map.Entry<Class<?>, State> stateEntry : session.getStatesCopy() )
        {
            JsonNode node = MAPPER.convertValue( stateEntry.getValue(), JsonNode.class );
            if ( node.isObject() && node.size() != 0 )
            {
                root.set( stateEntry.getKey().getSimpleName(), node );
            }
        }

        return MAPPER.writeValueAsString( root );
    }
}
