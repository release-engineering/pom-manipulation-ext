/*
 * Copyright (C) 2012 Red Hat, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.PropertyInterpolator;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.RangeResolverState;
import org.commonjava.maven.ext.core.state.State;
import org.commonjava.maven.ext.io.resolver.GalleyAPIWrapper;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.meta.MavenMetadataView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This Manipulator runs first and is active by default. It will resolve any ranges and update
 * them to locked values.
 */
@Named( "range-resolver" )
@Singleton
public class RangeResolver
                implements Manipulator
{
    private static final Logger logger = LoggerFactory.getLogger( RangeResolver.class );

    private ManipulationSession session;

    private final GalleyAPIWrapper readerWrapper;

    @Inject
    public RangeResolver( final GalleyAPIWrapper readerWrapper )
    {
        this.readerWrapper = readerWrapper;
    }

    @Override
    public void init( final ManipulationSession session )
    {
        this.session = session;
        session.setState( new RangeResolverState( session.getUserProperties() ) );
    }

    @Override
    public Set<Project> applyChanges( final List<Project> projects ) throws ManipulationException
    {
        final RangeResolverState state = session.getState( RangeResolverState.class );

        if ( !session.isEnabled() || !session.anyStateEnabled( State.activeByDefault ) || state == null
                || !state.isEnabled() )
        {
            logger.debug( "{}: Nothing to do!", getClass().getSimpleName() );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>( projects.size() );

        for ( final Project project : projects )
        {
            final PropertyInterpolator pi = new PropertyInterpolator( project.getModel().getProperties(), project );

            try {
                final Model model = project.getModel();
                final Build build = model.getBuild();

                if ( build  != null )
                {
                    final PluginManagement pluginManagement = build.getPluginManagement();

                    if ( pluginManagement != null )
                    {
                        final List<Plugin> plugins = pluginManagement.getPlugins();

                        for ( final Plugin plugin : plugins )
                        {
                            handleVersionWithRange( plugin, pi );
                        }
                    }

                    final List<Plugin> plugins = build.getPlugins();

                    if ( plugins != null )
                    {
                        for ( final Plugin plugin : plugins )
                        {
                            handleVersionWithRange( plugin, pi );
                        }
                    }
                }

                final DependencyManagement dependencyManagement = model.getDependencyManagement();

                if ( dependencyManagement != null )
                {
                    final List<Dependency> dependencies = dependencyManagement.getDependencies();

                    for ( final Dependency dependency : dependencies )
                    {
                        handleVersionWithRange( dependency, pi );
                    }
                }

                final List<Dependency> dependencies = model.getDependencies();

                for ( final Dependency dependency : dependencies )
                {
                    handleVersionWithRange( dependency, pi );
                }

                final List<Profile> profiles = model.getProfiles();

                for ( final Profile profile : profiles )
                {
                    final DependencyManagement profileDependencyManagement = profile.getDependencyManagement();

                    if ( profileDependencyManagement != null )
                    {
                        final List<Dependency> profileDependencyManagementDependencies
                                = profileDependencyManagement.getDependencies();

                        for ( final Dependency dependency : profileDependencyManagementDependencies )
                        {
                            handleVersionWithRange( dependency, pi );
                        }
                    }

                    final List<Dependency> profileDependencies = profile.getDependencies();

                    for ( final Dependency dependency : profileDependencies )
                    {
                        handleVersionWithRange( dependency, pi );
                    }

                    final BuildBase profileBuild = profile.getBuild();

                    if ( profileBuild != null )
                    {
                        final PluginManagement profilePluginManagement = profileBuild.getPluginManagement();

                        if ( profilePluginManagement != null )
                        {
                            final List<Plugin> plugins = profilePluginManagement.getPlugins();

                            for ( final Plugin plugin : plugins )
                            {
                                handleVersionWithRange( plugin, pi );
                            }
                        }

                        final List<Plugin> profilePlugins = profileBuild.getPlugins();

                        for ( final Plugin plugin : profilePlugins )
                        {
                            handleVersionWithRange( plugin, pi );
                        }
                    }
                }

                changed.add( project );
            }
            catch ( final RuntimeException e )
            {
                if ( e.getCause() instanceof ManipulationException )
                {
                    throw ( ManipulationException ) e.getCause();
                }
                throw e;
            }
        }
        return changed;
    }

    private void handleVersionWithRange( final Plugin plugin, final PropertyInterpolator pi )
            throws ManipulationException
    {
        final String version = pi.interp ( plugin.getVersion() );

        if ( StringUtils.isEmpty( version ) )
        {
            return;
        }

        try
        {
            final VersionRange versionRange = VersionRange.createFromVersionSpec( version );

            // If it's a range then try to use a matching version...
            if ( versionRange.hasRestrictions() )
            {
                final String groupId = pi.interp( plugin.getGroupId() );
                final String artifactId = pi.interp( plugin.getArtifactId() );
                final ProjectRef ref = new SimpleProjectRef( groupId, artifactId );
                final List<ArtifactVersion> versions = getVersions( ref );
                final ArtifactVersion result = versionRange.matchVersion( versions );

                logger.debug( "Resolved range for plugin {} got versionRange {} and potential replacement of {}",
                        plugin, versionRange, result );

                if ( result != null )
                {
                    plugin.setVersion( result.toString() );
                }
                else
                {
                    logger.warn( "Unable to find replacement for range." );
                }
            }
        }
        catch ( final InvalidVersionSpecificationException | ManipulationException e )
        {
            throw new ManipulationUncheckedException( new ManipulationException( "Invalid range", e ) );
        }
    }

    private void handleVersionWithRange( Dependency dependency, PropertyInterpolator pi ) throws ManipulationException
    {
        final String version = pi.interp( dependency.getVersion() );

        if ( StringUtils.isEmpty( version ) )
        {
            return;
        }

        try
        {
            final VersionRange versionRange = VersionRange.createFromVersionSpec( version );

            // If it's a range then try to use a matching version
            if ( versionRange.hasRestrictions() )
            {
                final String groupId = pi.interp( dependency.getGroupId() );
                final String artifactId = pi.interp( dependency.getArtifactId() );
                final ProjectRef ref = new SimpleProjectRef( groupId, artifactId );
                final List<ArtifactVersion> versions = getVersions( ref );
                final ArtifactVersion result = versionRange.matchVersion( versions );

                logger.debug( "Resolved range for dependency {} got versionRange {} and potential replacement of {}",
                        dependency, versionRange, result );

                if ( result != null )
                {
                    dependency.setVersion( result.toString() );
                }
                else
                {
                    logger.warn( "Unable to find replacement for range" );
                }
            }
        }
        catch ( final InvalidVersionSpecificationException | ManipulationException e )
        {
            throw new ManipulationUncheckedException( new ManipulationException( "Invalid range", e ) );
        }
    }

    private List<ArtifactVersion> getVersions( ProjectRef ga )
    {
        final MavenMetadataView mavenMetadataView;

        try
        {
            mavenMetadataView = readerWrapper.readMetadataView( ga );
        }
        catch ( final GalleyMavenException e )
        {
            final Throwable t = new ManipulationException( "Caught Galley exception processing artifact", e );
            throw new ManipulationUncheckedException( t );
        }

        return mavenMetadataView.resolveXPathToAggregatedStringList( "/metadata/versioning/versions/version", true, -1 )
                                .stream()
                                .distinct()
                                .map( DefaultArtifactVersion::new )
                                .collect( Collectors.toList() );
    }

    @Override
    public int getExecutionIndex()
    {
        // Low value index so it runs very early in order to lock the versions down prior to attempting REST alignment.
        return 2;
    }
}
