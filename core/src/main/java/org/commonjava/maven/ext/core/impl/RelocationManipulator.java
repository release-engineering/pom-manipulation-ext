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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.ProfileUtils;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.DependencyState;
import org.commonjava.maven.ext.core.state.PluginState;
import org.commonjava.maven.ext.core.state.RelocationState;
import org.commonjava.maven.ext.core.state.State;
import org.commonjava.maven.ext.core.util.WildcardMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.maven.ext.core.util.IdUtils.ga;

/**
 * {@link Manipulator} implementation that can relocation specified groupIds. It will also handle version changes by
 * delegating to dependencyExclusions.
 * Configuration is stored in a {@link PluginState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Named("relocations-manipulator")
@Singleton
public class RelocationManipulator
        implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    /**
     * Initialize the {@link PluginState} state holder in the {@link ManipulationSession}. This state holder detects
     * relocation configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later.
     */
    @Override
    public void init( final ManipulationSession session )
                    throws ManipulationException
    {
        this.session = session;
        session.setState( new RelocationState( session.getUserProperties() ) );
    }

    /**
     * Apply the relocation changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
            throws ManipulationException
    {
        final State state = session.getState( RelocationState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug("{}: Nothing to do!", getClass().getSimpleName());
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if ( apply( project, model ) )
            {
                changed.add( project );
            }
        }

        return changed;
    }

    private boolean apply( final Project project, final Model model ) throws ManipulationException
    {
        boolean result = false;
        final RelocationState state = session.getState( RelocationState.class );
        final WildcardMap<ProjectVersionRef> relocations = state.getDependencyRelocations();

        logger.debug( "Applying relocation changes to: " + ga( project ) );

        DependencyManagement dependencyManagement = model.getDependencyManagement();
        if ( dependencyManagement != null )
        {
            result = updateDependencies( relocations, project.getResolvedManagedDependencies( session ) );
        }
        result |= updateDependencies( relocations, project.getAllResolvedDependencies( session ) );

        for ( final Profile profile : ProfileUtils.getProfiles( session, model) )
        {
            dependencyManagement = profile.getDependencyManagement();
            if ( dependencyManagement != null )
            {
                result |= updateDependencies( relocations, project.getResolvedProfileManagedDependencies( session ).get( profile ) );
            }
            result |= updateDependencies( relocations, project.getAllResolvedProfileDependencies( session ).get( profile ) );

        }
        return result;
    }

    private boolean updateDependencies( WildcardMap<ProjectVersionRef> relocations, Map<ArtifactRef, Dependency> dependencies )
    {
        boolean result = false;
        final HashMap<ArtifactRef, Dependency> postFixUp = new HashMap<>(  );

        // If we do a single pass over the dependencies that will handle the relocations *but* it will not handle
        // where one relocation alters the dependency and a subsequent relocation alters it again. For instance, the
        // first might wildcard alter the groupId and the second, more specifically alters one with the artifactId
        for ( int i = 0 ; i < relocations.size(); i++ )
        {
            Iterator<ArtifactRef> it = dependencies.keySet().iterator();
            while ( it.hasNext() )
            {
                final ArtifactRef pvr = it.next();
                if ( relocations.containsKey( pvr.asProjectRef() ) )
                {
                    ProjectVersionRef relocation = relocations.get( pvr.asProjectRef() );
                    updateDependencyExclusion( pvr, relocation );

                    logger.info( "Replacing groupId {} by {} and artifactId {} with {}",
                                 dependencies.get( pvr ).getGroupId(), relocation.getGroupId(), dependencies.get( pvr ).getArtifactId(), relocation.getArtifactId() );

                    if ( !relocation.getArtifactId().equals( WildcardMap.WILDCARD ) )
                    {
                        dependencies.get( pvr ).setArtifactId( relocation.getArtifactId() );
                    }
                    dependencies.get( pvr ).setGroupId( relocation.getGroupId() );

                    // Unfortunately because we iterate using the resolved project keys if the relocation updates those
                    // keys multiple iterations will not work. Therefore we need to remove the original key:dependency
                    // to map to the relocated form.
                    postFixUp.put( SimpleArtifactRef.parse( dependencies.get( pvr ).getManagementKey() ), dependencies.get( pvr ) );
                    it.remove();

                    result = true;
                }
            }
            dependencies.putAll( postFixUp );
            postFixUp.clear();
        }
        return result;
    }

    /**
     * @param depPvr the resolved dependency we are processing the exclusion for.
     * @param relocation Map containing the update information for relocations.
     */
    private void updateDependencyExclusion( ProjectVersionRef depPvr, ProjectVersionRef relocation )
    {
        final DependencyState state = session.getState( DependencyState.class );

        if (relocation.getVersionString().equals( WildcardMap.WILDCARD ) )
        {
            logger.debug ("No version alignment to perform for relocations");
        }
        else
        {
            String artifact = depPvr.getArtifactId();
            if ( ! relocation.getArtifactId().equals( WildcardMap.WILDCARD ))
            {
                artifact = relocation.getArtifactId();
            }

            logger.debug ("Adding dependencyOverride {} & {}", relocation.getGroupId() + ':' + artifact + "@*",
                          relocation.getVersionString() );
            state.updateExclusions( relocation.getGroupId() + ':' + artifact + "@*", relocation.getVersionString() );
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 15;
    }
}
