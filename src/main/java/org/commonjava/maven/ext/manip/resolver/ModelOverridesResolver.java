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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.resolution.ModelResolver;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
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
@Component( role = ModelOverridesResolver.class )
public class ModelOverridesResolver
{
    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactResolver resolver;

    @Requirement
    private ModelBuilder modelBuilder;

    /**
     * Protected constructor for component instantiation/injection
     */
    protected ModelOverridesResolver()
    {

    }

    public Map<String, String> getRemoteDependencyVersionOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving dependency management GAV: " + gav );

        try
        {
            final Map<String, String> versionOverrides = new HashMap<String, String>();
            final Artifact artifact = resolvePom( gav, session );

            final ModelResolver modelResolver = this.newModelResolver( session );

            final Model effectiveModel = buildModel( artifact.getFile(), modelResolver );
            logger.debug( "Built model for project: " + effectiveModel.getName() );

            if ( effectiveModel.getDependencyManagement() == null )
            {
                throw new ManipulationException(
                                                 "Attempting to align to a BOM that does not have a dependencyManagement section" );
            }

            for ( final org.apache.maven.model.Dependency dep : effectiveModel.getDependencyManagement()
                                                                              .getDependencies() )
            {
                final String groupIdArtifactId = dep.getGroupId() + ":" + dep.getArtifactId();
                versionOverrides.put( groupIdArtifactId, dep.getVersion() );
                logger.debug( "Added version override for: " + groupIdArtifactId + ":" + dep.getVersion() );
            }

            return versionOverrides;
        }
        catch ( final ArtifactResolutionException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
        catch ( final ModelBuildingException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    public Map<String, String> getRemotePropertyMappingOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving remote property mapping POM: " + gav );

        try
        {
            final Artifact artifact = resolvePom( gav, session );

            final ModelResolver modelResolver = this.newModelResolver( session );

            final Model effectiveModel = buildModel( artifact.getFile(), modelResolver );

            final Properties versionOverrides = effectiveModel.getProperties();

            logger.debug( "Returning override of " + versionOverrides );

            return new HashMap<String, String>( (Map) versionOverrides );
        }
        catch ( final ArtifactResolutionException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
        catch ( final ModelBuildingException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
    }

    public Map<String, String> getRemotePluginVersionOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving remote plugin management POM: " + gav );

        try
        {
            final Artifact artifact = resolvePom( gav, session );

            final ModelResolver modelResolver = this.newModelResolver( session );

            final Model effectiveModel = buildModel( artifact.getFile(), modelResolver );

            final List<Plugin> plugins = effectiveModel.getBuild()
                                                       .getPluginManagement()
                                                       .getPlugins();

            final Map<String, String> versionOverrides = new HashMap<String, String>();

            for ( final Plugin plugin : plugins )
            {
                final String groupIdArtifactId = plugin.getGroupId() + ":" + plugin.getArtifactId();
                versionOverrides.put( groupIdArtifactId, plugin.getVersion() );
            }

            return versionOverrides;
        }
        catch ( final ArtifactResolutionException e )
        {
            throw new ManipulationException( "Unable to resolve artifact", e );
        }
        catch ( final ModelBuildingException e )
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
    private Model buildModel( final File pomFile, final ModelResolver modelResolver )
        throws ModelBuildingException
    {
        final ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile( pomFile );
        request.setModelResolver( modelResolver );
        request.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0 );
        request.setTwoPhaseBuilding( false ); // Resolve the complete model in one step
        request.setSystemProperties( System.getProperties() );
        final ModelBuildingResult result = modelBuilder.build( request );
        return result.getEffectiveModel();
    }

    /**
     * Resolve the pom file for a given GAV
     *
     * @param gav must be in the format groupId:artifactId:version
     * @return The resolved pom artifact
     * @throws ArtifactResolutionException
     */
    private Artifact resolvePom( final String gav, final ManipulationSession session )
        throws ArtifactResolutionException
    {
        final String[] gavParts = gav.split( ":" );
        final String groupId = gavParts[0];
        final String artifactId = gavParts[1];
        final String version = gavParts[2];
        final String extension = "pom";

        Artifact artifact = new DefaultArtifact( groupId, artifactId, extension, version );
        artifact = resolveArtifact( artifact, session );

        return artifact;
    }

    /**
     * Resolve artifact from the remote repository
     *
     * @param artifact
     * @return
     * @throws ArtifactResolutionException
     */
    private Artifact resolveArtifact( final Artifact artifact, final ManipulationSession session )
        throws ArtifactResolutionException
    {
        final ArtifactRequest request = new ArtifactRequest();
        request.setArtifact( artifact );

        final List<RemoteRepository> remotes = RepositoryUtils.toRepos( session.getRemoteRepositories() );
        request.setRepositories( remotes );

        final RepositorySystemSession repositorySession = session.getRepositorySystemSession();
        final ArtifactResult result = resolver.resolveArtifact( repositorySession, request );
        return result.getArtifact();
    }

    private ModelResolver newModelResolver( final ManipulationSession session )
    {
        final RemoteRepositoryManager repoMgr = new DefaultRemoteRepositoryManager();

        final List<RemoteRepository> remotes = RepositoryUtils.toRepos( session.getRemoteRepositories() );

        final ModelResolver modelResolver =
            new BasicModelResolver( session.getRepositorySystemSession(), resolver, repoMgr, remotes );

        return modelResolver;
    }
}
