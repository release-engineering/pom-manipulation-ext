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
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.activation.ProfileActivationException;
import org.apache.maven.project.ProjectBuilder;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.GAV;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.ProjectComparator;
import org.commonjava.maven.ext.core.impl.Manipulator;
import org.commonjava.maven.ext.core.state.CommonState;
import org.commonjava.maven.ext.core.state.State;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.core.util.ManipulatorPriorityComparator;
import org.commonjava.maven.ext.io.PomIO;
import org.commonjava.maven.ext.io.resolver.ExtensionInfrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.commonjava.maven.ext.common.util.ProfileUtils.PROFILE_SCANNING;
import static org.commonjava.maven.ext.common.util.ProfileUtils.PROFILE_SCANNING_DEFAULT;

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
@Named
@Singleton
public class ManipulationManager
{
    private static final String MARKER_PATH = "target";

    public static final String MARKER_FILE =  MARKER_PATH + File.separatorChar + "pom-manip-ext-marker.txt";

    public static final String RESULT_FILE = MARKER_PATH + File.separatorChar + "pom-manip-ext-result.json";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ProjectBuilder projectBuilder;

    private Map<String, Manipulator> manipulators;

    private Map<String, ExtensionInfrastructure> infrastructure;

    private final PomIO pomIO;

    @Inject
    public ManipulationManager( ProjectBuilder projectBuilder, Map<String, Manipulator> manipulators,
                                Map<String, ExtensionInfrastructure> infrastructure, PomIO pomIO)
    {
        this.projectBuilder = projectBuilder;
        this.manipulators = manipulators;
        this.infrastructure = infrastructure;
        this.pomIO = pomIO;
    }

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
        orderedManipulators.sort( Collections.reverseOrder( new ManipulatorPriorityComparator() ) );

        for ( final Manipulator manipulator : orderedManipulators )
        {
            logger.debug( "Initialising manipulator " + manipulator.getClass()
                                                                   .getSimpleName() );
            manipulator.init( session );
        }
        orderedManipulators.sort( new ManipulatorPriorityComparator() );

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
        final List<Project> currentProjects = pomIO.parseProject( session.getPom() );
        final List<Project> originalProjects = new ArrayList<>(  );
        currentProjects.forEach( p -> originalProjects.add( new Project( p ) ) );

        session.getActiveProfiles().addAll( parseActiveProfiles( session, currentProjects ) );
        session.setProjects( currentProjects );

        for ( final Project project : currentProjects )
        {
            logger.debug( "Got " + project + " (POM: " + project.getPom() + ")" );
        }

        Set<Project> changed = applyManipulations( currentProjects );

        // Create a marker file if we made some changes to prevent duplicate runs.
        if ( !changed.isEmpty() )
        {
            logger.info( "Maven-Manipulation-Extension: Rewrite changed: " + currentProjects );

            GAV gav = pomIO.rewritePOMs( changed );

            try
            {
                final VersioningState state = session.getState( VersioningState.class );
                state.setExecutionRootModified( gav );

                new File( session.getTargetDir().getParentFile(), ManipulationManager.MARKER_PATH ).mkdirs();

                new File( session.getTargetDir().getParentFile(), ManipulationManager.MARKER_FILE ).createNewFile();

                try (FileWriter writer = new FileWriter( new File( session.getTargetDir().getParentFile(), RESULT_FILE ) ))
                {
                    writer.write( collectResults( session ) );
                }
            }
            catch ( IOException e )
            {
                logger.error( "Unable to create marker or result file", e );
                throw new ManipulationException( "Marker/result file creation failed", e );
            }

            ProjectComparator.compareProjects( session, originalProjects, currentProjects );
        }

        // Ensure shutdown of GalleyInfrastructure Executor Service
        for ( ExtensionInfrastructure e : infrastructure.values() )
        {
            e.finish();
        }

        logger.info( "Maven-Manipulation-Extension: Finished." );
    }


    @SuppressWarnings( {"unchecked", "deprecation" } )
    private Set<String> parseActiveProfiles( ManipulationSession session, List<Project> projects ) throws ManipulationException
    {
        final Set<String> activeProfiles = new HashSet<>();
        final DefaultProfileManager dpm = new DefaultProfileManager( session.getSession().getContainer(), session.getUserProperties() );

        for ( Project p : projects )
        {
            // We clone the original profile here to prevent the DefaultProfileManager affecting the original list
            // during its activation calculation.
            p.getModel().getProfiles().stream().filter( newProfile -> ! dpm.getProfilesById().containsKey( newProfile.getId() ) ).
                            forEach( newProfile -> dpm.addProfile( newProfile.clone() ) );

            try
            {
                List<org.apache.maven.model.Profile> ap = dpm.getActiveProfiles();
                activeProfiles.addAll( ap.stream().map( org.apache.maven.model.Profile::getId ).collect( Collectors.toList() ) );
            }
            catch ( ProfileActivationException e )
            {
                throw new ManipulationException( "Activation detection failure", e );
            }
        }
        logger.debug( "Will {}scan all profiles and returning active profiles of {} ",
                      Boolean.parseBoolean( session.getUserProperties().getProperty( PROFILE_SCANNING, PROFILE_SCANNING_DEFAULT ) ) ? "not " : "",
                      activeProfiles );

        return activeProfiles;
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
     */
    private String collectResults( final ManipulationSession session )
                    throws JsonProcessingException
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
