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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This Manipulator runs first and is active by default. It will resolve any ranges and update
 * them to locked values.
 */
@Named("range-resolver")
@Singleton
public class RangeResolver
                implements Manipulator
{
    private static final Logger logger = LoggerFactory.getLogger( RangeResolver.class );

    private ManipulationSession session;

    private GalleyAPIWrapper readerWrapper;

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
        final Set<Project> changed = new HashSet<>();
        final RangeResolverState state = session.getState( RangeResolverState.class );

        if ( !session.isEnabled() || !session.anyStateEnabled( State.activeByDefault ) || state == null || !state.isEnabled() )
        {
            logger.debug("{}: Nothing to do!", getClass().getSimpleName());
            return Collections.emptySet();
        }

        for ( Project p : projects )
        {
            try
            {
                // TODO:
                // Ranges in properties are not currently handled.

                if ( p.getModel().getBuild() != null )
                {
                    // PluginManagement
                    if ( p.getModel().getBuild().getPluginManagement() != null )
                    {
                        p.getModel()
                         .getBuild()
                         .getPluginManagement()
                         .getPlugins()
                         .stream()
                         .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                         .forEach( this::handleVersionWithRange );
                    }
                    // Plugins
                    p.getModel()
                     .getBuild()
                     .getPlugins()
                     .stream()
                     .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                     .forEach( this::handleVersionWithRange );
                }

                // DependencyManagement
                if ( p.getModel().getDependencyManagement() != null )
                {
                    p.getModel().getDependencyManagement().getDependencies().stream()
                     .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                     .forEach( this::handleVersionWithRange );
                }
                // Dependencies
                p.getModel().getDependencies().stream()
                 .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                 .forEach( this::handleVersionWithRange );

                asStream( p.getModel().getProfiles() ).forEach( profile -> {
                    // DependencyManagement
                    if ( profile.getDependencyManagement() != null )
                    {
                        profile.getDependencyManagement().getDependencies().stream()
                         .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                         .forEach( this::handleVersionWithRange );
                    }
                    // Dependencies
                    profile.getDependencies().stream()
                     .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                     .forEach( this::handleVersionWithRange );

                    if ( profile.getBuild() != null )
                    {
                        // PluginManagement
                        if ( profile.getBuild().getPluginManagement() != null )
                        {
                            profile.getBuild()
                             .getPluginManagement()
                             .getPlugins()
                             .stream()
                             .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                             .forEach( this::handleVersionWithRange );
                        }
                        // Plugins
                        profile.getBuild()
                         .getPlugins()
                         .stream()
                         .filter( d -> StringUtils.isNotEmpty( d.getVersion() ) )
                         .forEach( this::handleVersionWithRange );
                    }
                } );

                changed.add( p );
            }
            catch ( RuntimeException e )
            {
                if ( e.getCause() instanceof ManipulationException )
                {
                    throw (ManipulationException)e.getCause();
                }
                throw e;
            }
        }
        return changed;
    }

    private void handleVersionWithRange( Plugin p )
    {
        try
        {
            VersionRange versionRange = VersionRange.createFromVersionSpec( p.getVersion() );

            // If its a range then try to use a matching version...
            if ( versionRange.hasRestrictions() )
            {
                final List<ArtifactVersion> versions = getVersions( new SimpleProjectRef( p.getGroupId(), p.getArtifactId() ) );
                final ArtifactVersion result = versionRange.matchVersion( versions );

                logger.debug( "Resolved range for plugin {} got versionRange {} and potential replacement of {} ", p, versionRange, result );

                if ( result != null )
                {
                    p.setVersion( result.toString() );
                }
                else
                {
                    logger.warn( "Unable to find replacement for range." );
                }
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new RuntimeException( "UNCHECKED LAMBDA", new ManipulationException( "Invalid range", e ) );
        }
    }

    private void handleVersionWithRange( Dependency d )
    {
        try
        {
            VersionRange versionRange = VersionRange.createFromVersionSpec( d.getVersion() );

            // If its a range then try to use a matching version...
            if ( versionRange.hasRestrictions() )
            {
                final List<ArtifactVersion> versions = getVersions( new SimpleProjectRef( d.getGroupId(), d.getArtifactId() ) );
                final ArtifactVersion result = versionRange.matchVersion( versions );

                logger.debug( "Resolved range for dependency {} got versionRange {} and potential replacement of {} ", d, versionRange, result );

                if ( result != null )
                {
                    d.setVersion( result.toString() );
                }
                else
                {
                    logger.warn( "Unable to find replacement for range." );
                }
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new RuntimeException( "UNCHECKED LAMBDA", new ManipulationException( "Invalid range", e ) );
        }
    }

    private List<ArtifactVersion> getVersions( ProjectRef ga )
    {
        MavenMetadataView mavenMetadataView;
        try
        {
            mavenMetadataView = readerWrapper.readMetadataView( ga );
        }
        catch ( GalleyMavenException e )
        {
            throw new RuntimeException( "UNCHECKED-LAMBDA", new ManipulationException( "Caught Galley exception processing artifact", e ) );
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

    private static Stream<Profile> asStream ( final Collection<Profile> collection)
    {
        return ( collection == null ? Stream.empty() : collection.stream() );
    }
}
