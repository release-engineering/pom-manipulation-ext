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
/**
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.manip.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.resolution.ModelResolver;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.impl.RemoteRepositoryManager;
import org.sonatype.aether.impl.internal.DefaultRemoteRepositoryManager;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Class to resolve artifact descriptors (pom files) from a maven repository
 */
public class EffectiveModelBuilder
{
    private static EffectiveModelBuilder instance;

    private Logger logger;

    private MavenSession session;

    private ArtifactResolver resolver;

    private ModelBuilder modelBuilder;

    /**
     * Repositories for downloading remote poms
     */
    private List<RemoteRepository> repositories;

    /**
     * Get list of remote repositories from which to download artifacts
     *
     * @return list of repositories
     */
    private List<RemoteRepository> getRepositories()
    {
        if ( repositories == null )
        {
            repositories = new ArrayList<RemoteRepository>();
        }

        return repositories;
    }

    /**
     * Set the list of remote repositories from which to download dependency management poms.
     *
     * @param repository
     */
    public void addRepository( ArtifactRepository repository )
    {
        RemoteRepository remoteRepo = new RemoteRepository( repository.getId(), "default", repository.getUrl() );
        getRepositories().add( remoteRepo );
    }

    /**
     * Private constructor for singleton
     */
    private EffectiveModelBuilder()
    {

    }

    public static void init( Logger logger, MavenSession session, ArtifactResolver resolver, ModelBuilder modelBuilder )
        throws ComponentLookupException, PlexusContainerException
    {
        instance = new EffectiveModelBuilder();
        instance.logger = logger;
        instance.session = session;
        instance.resolver = resolver;
        instance.modelBuilder = modelBuilder;
        initRepositories( session.getRequest()
                                 .getRemoteRepositories() );
    }

    /**
     * Initialize the set of repositories from which to download remote artifacts
     *
     * @param repositories
     */
    private static void initRepositories( List<ArtifactRepository> repositories )
    {
        if ( repositories == null || repositories.size() == 0 )
        {
            // Set default repository list to include Maven central
            String remoteRepoUrl = "http://repo.maven.apache.org/maven2";
            instance.getRepositories()
                    .add( new RemoteRepository( "central", "default", remoteRepoUrl ) );
        }
        for ( ArtifactRepository artifactRepository : repositories )
        {
            instance.addRepository( artifactRepository );
        }
    }

    /**
     * Return the instance. Will return "null" until init() has been called.
     *
     * @return the initialized instance or null if it hasn't been initialized yet
     */
    public static EffectiveModelBuilder getInstance()
    {
        return instance;
    }

    public Map<String, String> getRemoteDependencyVersionOverrides( String gav )
        throws ManipulationException
    {
        logger.debug( "Resolving dependency management GAV: " + gav );

        try
        {
            Map<String, String> versionOverrides = new HashMap<String, String>();
            Artifact artifact = resolvePom( gav );

            ModelResolver modelResolver = this.newModelResolver();

            Model effectiveModel = buildModel( artifact.getFile(), modelResolver );
            logger.debug( "Built model for project: " + effectiveModel.getName() );

            if ( effectiveModel.getDependencyManagement() == null )
            {
                throw new ManipulationException(
                                                 "Attempting to align to a BOM that does not have a dependencyManagement section" );
            }

            for ( org.apache.maven.model.Dependency dep : effectiveModel.getDependencyManagement()
                                                                        .getDependencies() )
            {
                String groupIdArtifactId = dep.getGroupId() + ":" + dep.getArtifactId();
                versionOverrides.put( groupIdArtifactId, dep.getVersion() );
                logger.debug( "Added version override for: " + groupIdArtifactId + ":" + dep.getVersion() );
            }

            return versionOverrides;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
        catch ( ModelBuildingException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public Map<String, String> getRemotePropertyMappingOverrides( String gav )
        throws ManipulationException
    {
        logger.debug( "Resolving remote property mapping POM: " + gav );

        try
        {
            Artifact artifact = resolvePom( gav );

            ModelResolver modelResolver = this.newModelResolver();

            Model effectiveModel = buildModel( artifact.getFile(), modelResolver );

            Properties versionOverrides = effectiveModel.getProperties();

            logger.debug( "Returning override of " + versionOverrides );

            return new HashMap<String, String>( (Map) versionOverrides );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
        catch ( ModelBuildingException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
    }

    public Map<String, String> getRemotePluginVersionOverrides( String gav )
        throws ManipulationException
    {
        logger.debug( "Resolving remote plugin management POM: " + gav );

        try
        {
            Artifact artifact = resolvePom( gav );

            ModelResolver modelResolver = this.newModelResolver();

            Model effectiveModel = buildModel( artifact.getFile(), modelResolver );

            List<Plugin> plugins = effectiveModel.getBuild()
                                                 .getPluginManagement()
                                                 .getPlugins();

            Map<String, String> versionOverrides = new HashMap<String, String>();

            for ( Plugin plugin : plugins )
            {
                String groupIdArtifactId = plugin.getGroupId() + ":" + plugin.getArtifactId();
                versionOverrides.put( groupIdArtifactId, plugin.getVersion() );
            }

            return versionOverrides;
        }
        catch ( ArtifactResolutionException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
        catch ( ModelBuildingException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
    }

    /**
     * Build the effective model for the given pom file
     *
     * @param pomFile
     * @return effective pom model
     * @throws ModelBuildingException
     */
    private Model buildModel( File pomFile, ModelResolver modelResolver )
        throws ModelBuildingException
    {
        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile( pomFile );
        request.setModelResolver( modelResolver );
        request.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );
        request.setTwoPhaseBuilding( false ); // Resolve the complete model in one step
        request.setSystemProperties( System.getProperties() );
        ModelBuildingResult result = modelBuilder.build( request );
        return result.getEffectiveModel();
    }

    /**
     * Resolve the pom file for a given GAV
     *
     * @param gav must be in the format groupId:artifactId:version
     * @return The resolved pom artifact
     * @throws ArtifactResolutionException
     */
    private Artifact resolvePom( String gav )
        throws ArtifactResolutionException
    {
        String[] gavParts = gav.split( ":" );
        String groupId = gavParts[0];
        String artifactId = gavParts[1];
        String version = gavParts[2];
        String extension = "pom";

        Artifact artifact = new DefaultArtifact( groupId, artifactId, extension, version );
        artifact = resolveArtifact( artifact );

        return artifact;
    }

    /**
     * Resolve artifact from the remote repository
     *
     * @param artifact
     * @return
     * @throws ArtifactResolutionException
     */
    private Artifact resolveArtifact( Artifact artifact )
        throws ArtifactResolutionException
    {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact( artifact );
        request.setRepositories( getRepositories() );

        RepositorySystemSession repositorySession = session.getRepositorySession();
        ArtifactResult result = resolver.resolveArtifact( repositorySession, request );
        return result.getArtifact();
    }

    private ModelResolver newModelResolver()
    {
        RemoteRepositoryManager repoMgr = new DefaultRemoteRepositoryManager();
        ModelResolver modelResolver =
            new BasicModelResolver( session.getRepositorySession(), resolver, repoMgr, getRepositories() );

        return modelResolver;
    }
}
