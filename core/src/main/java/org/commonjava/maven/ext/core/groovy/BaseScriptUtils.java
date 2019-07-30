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
package org.commonjava.maven.ext.core.groovy;

import groovy.lang.Script;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.PropertyResolver;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.impl.Version;
import org.commonjava.maven.ext.core.state.RESTState;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.commonjava.maven.ext.io.rest.Translator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that contains useful utility functions for developers wishing to implement groovy scripts
 * for PME.
 */
public abstract class BaseScriptUtils extends Script implements BaseScriptAPI
{
    protected final Logger logger = LoggerFactory.getLogger( BaseScriptUtils.class );

    /**
     * Allows the specified group:artifact property to be inlined. This is useful to split up properties that cover multiple separate projects.
     * @param currentProject The current project we are operating on.
     * @param groupArtifact A ProjectRef corresponding to the group and artifact of the dependency (or managed dependency) that we wish to inline.
     * @throws ManipulationException if an error occurs.
     */
    public void inlineProperty ( Project currentProject, ProjectRef groupArtifact ) throws ManipulationException
    {
        try
        {
            currentProject.getResolvedManagedDependencies( getSession() )
                          .entrySet().stream()
                          .filter( a -> a.getKey().asProjectRef().equals( groupArtifact ) && a.getValue().getVersion().contains( "$" ) )
                          .forEach( a -> {
                              logger.info( "Found managed artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( getSession(), currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
            currentProject.getResolvedDependencies( getSession() )
                          .entrySet().stream()
                          .filter( a -> a.getKey().asProjectRef().equals( groupArtifact ) && a.getValue().getVersion().contains( "$" ) )
                          .forEach( a -> {
                              logger.info( "Found artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( getSession(), currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
        }
        catch (ManipulationUncheckedException e)
        {
            throw (ManipulationException)e.getCause();
        }
    }

    /**
     * Allows the specified property to be inlined. This is useful to split up properties that cover multiple separate projects.

     * @param currentProject The current project we are operating on.
     * @param propertyKey The property which is within the dependencies (or managed dependencies) that we wish to inline.
     * @throws ManipulationException if an error occurs.
     */
    public void inlineProperty ( Project currentProject, String propertyKey ) throws ManipulationException
    {
        try
        {
            currentProject.getResolvedManagedDependencies( getSession() )
                          .entrySet().stream()
                          .filter( a -> a.getValue().getVersion().equals( "${" + propertyKey + "}" ) )
                          .forEach( a -> {
                              logger.info( "Found managed artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( getSession(), currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
            currentProject.getResolvedDependencies( getSession() )
                          .entrySet().stream()
                          .filter( a -> a.getValue().getVersion().equals( "${" + propertyKey + "}" ) )
                          .forEach( a -> {
                              logger.info( "Found artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( getSession(), currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
        }
        catch (ManipulationUncheckedException e)
        {
            throw (ManipulationException)e.getCause();
        }
    }


    private void validateSession() throws ManipulationException
    {
        if ( ! ( getSession() instanceof ManipulationSession ) )
        {
            throw new ManipulationException( "Unable to access session instance in " + getSession() );
        }
    }

    /**
     * This will re-initialise any state linked to this session. This is useful if the user properties have been
     * updated.
     */
    public void reinitialiseSessionStates() throws ManipulationException
    {
        validateSession();
        ((ManipulationSession)getSession()).reinitialiseStates();
    }


    // TODO: Does this need to be public/protected?
    private Translator getRESTAPI() throws ManipulationException
    {
        validateSession();
        RESTState rs = ((ManipulationSession)getSession()).getState( RESTState.class );
        return rs.getVersionTranslator();
    }

    /**
     * This is useful for a series of builds with circular dependencies. It will allow a developer
     * to use an original 'target' build to align the project version to. The engineer should pass
     * in the unaligned GAV (i.e. without the rebuild suffix) of the first SCM root build. Then the
     * subsequent builds, rather than using incremental suffix which can cause issues with circular
     * dependencies, will use versionSuffix and lock to the root build version. This function will query
     * DA to obtain the correct current suffix to use.
     *
     * @param gav the target build to obtain the suffix to align to.
     * @throws ManipulationException if an error occurs.
     */
    public void overrideProjectVersion (ProjectVersionRef gav) throws ManipulationException
    {
        // Wrapper function to locate latest build of group:artifact:version and then replace the incrementalSuffix
        // by a versionSuffix instead.

        List<ProjectVersionRef> source = new ArrayList<>();
        source.add( gav );
        source.add( getGAV() );

        Map<ProjectVersionRef, String> restResult = getRESTAPI().translateVersions( source );
        String targetBuild = restResult.get( gav );
        String thisMapping = restResult.get( getGAV() );

        if ( targetBuild == null )
        {
            logger.error( "REST result was {}", restResult );
            throw new ManipulationException( "Multiple results returned ; unable to reset version." );
        }
        if ( thisMapping != null )
        {
            // TODO: Validate that this project < target gav
            logger.debug( "Comparing target GAV build {} to this build {} ", targetBuild, thisMapping );
            if ( Version.getIntegerBuildNumber( thisMapping ) < Version.getIntegerBuildNumber( targetBuild ) )
            {
                logger.error( "Alignment failure: Target is {} and this is {}", Version.getIntegerBuildNumber( thisMapping ), Version.getIntegerBuildNumber( targetBuild ) );
                throw new ManipulationException( "Unable to set version suffix as dependent build has been built more times than the original target" );
            }
        }

        String newSuffix = targetBuild.substring( gav.getVersionString().length() + 1 );
        logger.info ("From version {}, updating versionSuffix to {}", targetBuild, newSuffix );
        getUserProperties().put( VersioningState.VERSION_SUFFIX_SYSPROP.getCurrent(), newSuffix );

        reinitialiseSessionStates();
    }
}
