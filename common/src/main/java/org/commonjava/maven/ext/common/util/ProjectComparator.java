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

package org.commonjava.maven.ext.common.util;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.session.MavenSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.DEPENDENCIES;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.DEPENDENCIES_UNVERSIONED;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.MANAGED_DEPENDENCIES;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.MANAGED_PLUGINS;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.PLUGINS;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.PROFILE_DEPENDENCIES;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.PROFILE_DEPENDENCIES_UNVERSIONED;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.PROFILE_MANAGED_DEPENDENCIES;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.PROFILE_MANAGED_PLUGINS;
import static org.commonjava.maven.ext.common.util.ProjectComparator.Type.PROFILE_PLUGINS;

public class ProjectComparator
{
    private static final String REPORT_NON_ALIGNED = "reportNonAligned";

    private static final Logger logger = LoggerFactory.getLogger( ProjectComparator.class );

    private static final AtomicBoolean spacerLine = new AtomicBoolean();

    enum Type
    {
        DEPENDENCIES,
        DEPENDENCIES_UNVERSIONED,
        MANAGED_DEPENDENCIES,
        PROFILE_DEPENDENCIES,
        PROFILE_DEPENDENCIES_UNVERSIONED,
        PROFILE_MANAGED_DEPENDENCIES,
        PLUGINS,
        MANAGED_PLUGINS,
        PROFILE_PLUGINS,
        PROFILE_MANAGED_PLUGINS;

        @Override
        public String toString()
        {
            switch ( this )
            {
                case DEPENDENCIES:
                    return "Dependencies";
                case DEPENDENCIES_UNVERSIONED:
                    return "Non-versioned dependencies";
                case MANAGED_DEPENDENCIES:
                    return "Managed dependencies";
                case PROFILE_DEPENDENCIES:
                    return "Profile dependencies";
                case PROFILE_DEPENDENCIES_UNVERSIONED:
                    return "Profile non-versioned dependencies";
                case PROFILE_MANAGED_DEPENDENCIES:
                    return "Profile managed dependencies";
                case PLUGINS:
                    return "Plugins";
                case MANAGED_PLUGINS:
                    return "Managed plugins";
                case PROFILE_PLUGINS:
                    return "Profile plugins";
                case PROFILE_MANAGED_PLUGINS:
                    return "Profile managed plugins";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    public static void compareProjects( MavenSessionHandler session,
                                        WildcardMap<ProjectVersionRef> dependencyRelocations, List<Project> originalProjects, List<Project> newProjects )
                    throws ManipulationException
    {
        final boolean reportNonAligned = Boolean.parseBoolean( session.getUserProperties().getProperty( REPORT_NON_ALIGNED, "false") );

        try
        {
            newProjects.forEach(
                        newProject -> originalProjects.stream().
                                        filter( originalProject -> newProject.getArtifactId().equals( originalProject.getArtifactId() )
                                                                           && newProject.getGroupId().equals( originalProject.getGroupId() ) ).forEach( originalProject ->
                        {
                            logger.info( "------------------- project {} ", newProject.getKey().asProjectRef() );
                            if ( ! originalProject.getVersion().equals( newProject.getVersion() ) )
                            {
                                logger.info( "\tProject version : {} ---> {}", originalProject.getVersion(), newProject.getVersion() );
                                spacerLine.set( true );
                            }
                            injectSpacerLine();

                            newProject.getModel().getProperties().forEach( ( nKey, nValue ) ->
                                originalProject.getModel().getProperties().forEach( ( oKey, oValue ) -> {
                                    if ( oKey != null && oKey.equals( nKey ) &&  oValue != null &&  !oValue.equals( nValue ) )
                                    {
                                        logger.info( "\tProperty : key {} ; value {} ---> {}", oKey, oValue, nValue );
                                        spacerLine.set( true );
                                    }
                                } )
                            );
                            injectSpacerLine();

                            compareDependencies( DEPENDENCIES,
                                                 dependencyRelocations,
                                                 reportNonAligned,
                                                 handleDependencies( session, originalProject, null, DEPENDENCIES ),
                                                 handleDependencies( session, newProject, null, DEPENDENCIES ) );

                            injectSpacerLine();

                            compareDependencies( MANAGED_DEPENDENCIES, dependencyRelocations, reportNonAligned,
                                                 handleDependencies( session, originalProject, null,
                                                                     MANAGED_DEPENDENCIES ),
                                                 handleDependencies( session, newProject, null, MANAGED_DEPENDENCIES ) );

                            injectSpacerLine();

                            compareDependencies( DEPENDENCIES_UNVERSIONED, dependencyRelocations, reportNonAligned,
                                                 handleDependencies( session, originalProject, null, DEPENDENCIES_UNVERSIONED ),
                                                 handleDependencies( session, newProject, null, DEPENDENCIES_UNVERSIONED ) );

                            injectSpacerLine();

                            comparePlugins( PLUGINS,
                                                 reportNonAligned,
                                                 handlePlugins( session, originalProject, null, PLUGINS ),
                                                 handlePlugins( session, newProject, null, PLUGINS ) );
                            comparePlugins( MANAGED_PLUGINS, reportNonAligned,
                                            handlePlugins( session, originalProject, null,
                                                           MANAGED_PLUGINS ),
                                            handlePlugins( session, newProject, null, MANAGED_PLUGINS ) );

                            List<Profile> oldProfiles = ProfileUtils.getProfiles( session, originalProject.getModel() );
                            List<Profile> newProfiles = ProfileUtils.getProfiles( session, newProject.getModel() );

                            newProfiles.forEach( newProfile -> oldProfiles.stream().
                                            filter( oldProfile -> newProfile.getId().equals( oldProfile.getId() ) ).
                                                                                          forEach( oldProfile ->
                            {
                                newProfile.getProperties().forEach( ( nKey, nValue ) ->
                                    oldProfile.getProperties().forEach( ( oKey, oValue ) -> {
                                        if ( oKey != null && oKey.equals( nKey ) &&  oValue != null &&  !oValue.equals( nValue ) )
                                        {
                                            logger.info( "\tProfile property : key {} ; value {} ---> {}", oKey, oValue, nValue );
                                            spacerLine.set( true );
                                        }
                                    } )
                                );

                                injectSpacerLine();

                                compareDependencies( PROFILE_DEPENDENCIES, dependencyRelocations, reportNonAligned,
                                                     handleDependencies( session, originalProject, oldProfile, PROFILE_DEPENDENCIES ),
                                                     handleDependencies( session, newProject, newProfile, PROFILE_DEPENDENCIES ) );

                                injectSpacerLine();

                                compareDependencies( PROFILE_MANAGED_DEPENDENCIES, dependencyRelocations,
                                                     reportNonAligned,
                                                     handleDependencies( session, originalProject,
                                                                         oldProfile,
                                                                         PROFILE_MANAGED_DEPENDENCIES ),
                                                     handleDependencies( session, newProject, newProfile,
                                                                         PROFILE_MANAGED_DEPENDENCIES ) );

                                injectSpacerLine();

                                compareDependencies( PROFILE_DEPENDENCIES_UNVERSIONED, dependencyRelocations,
                                                     reportNonAligned,
                                                     handleDependencies( session, originalProject, oldProfile, PROFILE_DEPENDENCIES_UNVERSIONED ),
                                                     handleDependencies( session, newProject, newProfile, PROFILE_DEPENDENCIES_UNVERSIONED ) );

                                injectSpacerLine();

                                comparePlugins( PROFILE_PLUGINS, reportNonAligned,
                                                handlePlugins( session, originalProject, oldProfile, PROFILE_PLUGINS ),
                                                handlePlugins( session, newProject, newProfile, PROFILE_PLUGINS ) );

                                injectSpacerLine();

                                comparePlugins( PROFILE_MANAGED_PLUGINS, reportNonAligned,
                                                handlePlugins( session, originalProject,
                                                               oldProfile,
                                                               PROFILE_MANAGED_PLUGINS ),
                                                handlePlugins( session, newProject, newProfile,
                                                                         PROFILE_MANAGED_PLUGINS ) );
                            } ) );
                        } ) );
        }
        catch (ManipulationUncheckedException e)
        {
            throw (ManipulationException)e.getCause();
        }
    }


    private static void compareDependencies( Type type, WildcardMap<ProjectVersionRef> dependencyRelocations, boolean reportNonAligned, Set<ArtifactRef> originalDeps,
                                             Set<ArtifactRef> newDeps )
    {
        Set<ArtifactRef> nonAligned = new HashSet<>();

        if ( dependencyRelocations.size() > 0 && type == PROFILE_DEPENDENCIES_UNVERSIONED
                        || type == DEPENDENCIES_UNVERSIONED )
        {
            originalDeps.forEach( originalDep -> {
                ProjectRef orig = originalDep.asProjectRef();
                if ( dependencyRelocations.containsKey( orig ) )
                {
                    ProjectVersionRef p = dependencyRelocations.get( orig );
                    logger.info( "\tUnversioned relocation : {} ---> {}:{}:{}", originalDep, p.getGroupId(),
                                 p.getArtifactId().equals( "*" ) ? orig.getArtifactId() : p.getArtifactId(), p.getVersionString() );
                    spacerLine.set( true );
                }
            } );
            injectSpacerLine();
        }
        else
        {
            originalDeps.forEach( originalArtifact -> newDeps.stream()
                                                             .filter( newArtifact -> ( newArtifact.getGroupId().equals( originalArtifact.getGroupId() )
                                                                             && newArtifact.getArtifactId().equals( originalArtifact.getArtifactId() )
                                                                             && newArtifact.getType().equals( originalArtifact.getType() )
                                                                             && StringUtils.equals( newArtifact.getClassifier(),
                                                                                                    originalArtifact.getClassifier() ) ) )
                                                             .forEach( newArtifact -> {
                                                                 if ( !newArtifact.getVersionString().equals( originalArtifact.getVersionString() ) )
                                                                 {
                                                                     logger.info( "\t{} : {} --> {} ", type,
                                                                                  originalArtifact, newArtifact );
                                                                     spacerLine.set( true );
                                                                 }
                                                                 else if ( reportNonAligned )
                                                                 {
                                                                     nonAligned.add( originalArtifact );
                                                                 }
                                                             } ) );

            if ( dependencyRelocations.size() > 0 )
            {
                injectSpacerLine();

                originalDeps.forEach( originalDep -> {
                    ProjectRef orig = originalDep.asProjectRef();
                    if ( dependencyRelocations.containsKey( orig ) )
                    {
                        ProjectVersionRef p = dependencyRelocations.get( orig );
                        logger.info( "\tRelocation : {} ---> {}:{}:{}", originalDep, p.getGroupId(),
                                     p.getArtifactId().equals( "*" ) ? orig.getArtifactId() : p.getArtifactId(), p.getVersionString() );
                        spacerLine.set( true );
                    }
                } );
            }
        }

        if ( nonAligned.size() > 0 )
        {
            nonAligned.forEach( na -> logger.info( "\tNon-Aligned {} : {} ", type, na ) );
            logger.info( "" );
        }
    }


    private static void comparePlugins( Type type, boolean reportNonAligned, Set<ProjectVersionRef> originalPlugins, Set<ProjectVersionRef> newPlugins )
    {
        Set<ProjectVersionRef> nonAligned = new HashSet<>( );
        AtomicBoolean spacerLine = new AtomicBoolean();

        originalPlugins.forEach( originalPVR -> newPlugins.stream().filter(
                        newPVR -> ( newPVR.getGroupId().equals( originalPVR.getGroupId() ) &&
                                        newPVR.getArtifactId().equals( originalPVR.getArtifactId() ) ) ).forEach( newArtifact ->
                {
                    if ( ! newArtifact.getVersionString().equals( originalPVR.getVersionString() ) )
                    {
                        logger.info( "\t{} : {} --> {} ", type, originalPVR, newArtifact );
                        spacerLine.set( true );
                    }
                    else if ( reportNonAligned )
                    {
                        nonAligned.add( originalPVR );
                    }
                }
        ) );

        if ( nonAligned.size() > 0 )
        {
            nonAligned.forEach( pv -> logger.info( "\tNon-Aligned {} : {} ", type, pv ) );
            logger.info( "" );
        }
    }

    private static Set<ArtifactRef> handleDependencies( MavenSessionHandler session, Project project, Profile profile,
                                                        Type type )
                    throws ManipulationUncheckedException
    {
        try
        {
            switch (type)
            {
                case DEPENDENCIES:
                {
                    return project.getResolvedDependencies( session ).keySet();
                }
                case MANAGED_DEPENDENCIES:
                {
                    return project.getResolvedManagedDependencies( session ).keySet();
                }
                case PROFILE_DEPENDENCIES:
                {
                    return project.getResolvedProfileDependencies( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                case PROFILE_MANAGED_DEPENDENCIES:
                {
                    return project.getResolvedProfileManagedDependencies( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                case DEPENDENCIES_UNVERSIONED:
                {
                    return project.getAllResolvedDependencies( session ).
                                                  keySet().stream().filter( a -> a.getVersionString().equals( "*" ) ).collect( Collectors.toSet() );
                }
                case PROFILE_DEPENDENCIES_UNVERSIONED:
                {
                    return project.getAllResolvedProfileDependencies( session ).
                                    getOrDefault( profile, Collections.emptyMap() ).
                                                  keySet().stream().filter( a -> a.getVersionString().equals( "*" ) ).collect( Collectors.toSet() );
                }
                default:
                {
                    throw new ManipulationException( "Invalid type " + type.toString() );
                }
            }
        }
        catch ( ManipulationException e )
        {
            throw new ManipulationUncheckedException( e );
        }
    }

    private static Set<ProjectVersionRef> handlePlugins( MavenSessionHandler session, Project project, Profile profile,
                                                        Type type )
                    throws ManipulationUncheckedException
    {
        try
        {
            switch (type)
            {
                case PLUGINS:
                {
                    return project.getResolvedPlugins( session ).keySet();
                }
                case MANAGED_PLUGINS:
                {
                    return project.getResolvedManagedPlugins( session ).keySet();
                }
                case PROFILE_PLUGINS:
                {
                    return project.getResolvedProfilePlugins( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                case PROFILE_MANAGED_PLUGINS:
                {
                    return project.getResolvedProfileManagedPlugins( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                default:
                {
                    throw new ManipulationException( "Invalid type " + type.toString() );
                }
            }
        }
        catch ( ManipulationException e )
        {
            throw new ManipulationUncheckedException( e );
        }
    }

    private static void injectSpacerLine()
    {
        if ( spacerLine.get() )
        {
            logger.info( "" );
            spacerLine.set( false );
        }
    }
}
