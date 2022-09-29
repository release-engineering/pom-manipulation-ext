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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.DependencyInjectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link Manipulator} implementation that can inject specified GAV into a project's top
 * level pom file in a dependencyManagement block.
 * Configuration is stored in a {@link DependencyInjectionState} instance, which is in turn stored
 * in the {@link ManipulationSession}.
 */
@Named("dependency-injection-manipulator")
@Singleton
public class DependencyInjectionManipulator
        implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    /**
     * Initialize the {@link DependencyInjectionState} state holder in the {@link ManipulationSession}. This state holder
     * detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        session.setState( new DependencyInjectionState( session.getUserProperties() ) );
        this.session = session;
    }

    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
            throws ManipulationException
    {
        final DependencyInjectionState state = session.getState( DependencyInjectionState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( "{}: Nothing to do!", getClass().getSimpleName() );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();

        projects.stream().filter( Project::isInheritanceRoot ).forEach( project -> {
            logger.info( "Applying injection changes to {}", project );

            DependencyManagement dependencyManagement = project.getModel().getDependencyManagement();
            if ( dependencyManagement == null )
            {
                dependencyManagement = new DependencyManagement();
                project.getModel().setDependencyManagement( dependencyManagement );
                logger.debug( "Added <DependencyManagement/> for current project" );
            }
            dependencyManagement.getDependencies().addAll( 0, getDependencies( state.getDependencyInjection() ));
            changed.add( project );
        } );

        return changed;
    }
    private List<Dependency> getDependencies( List<ProjectVersionRef> pvr )
    {
        List<Dependency> results = new ArrayList<>(  );

        for ( ProjectVersionRef p : pvr )
        {
            Dependency d = new Dependency();
            d.setGroupId( p.getGroupId() );
            d.setArtifactId( p.getArtifactId() );
            d.setVersion( p.getVersionString() );
            results.add( d );
        }
        return results;
    }

    @Override
    public int getExecutionIndex()
    {
        return 8;
    }
}
