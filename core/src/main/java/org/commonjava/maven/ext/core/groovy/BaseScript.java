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
import org.commonjava.maven.ext.common.session.MavenSessionHandler;
import org.commonjava.maven.ext.common.util.PropertyResolver;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.impl.InitialGroovyManipulator;
import org.commonjava.maven.ext.io.ModelIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Abstract class that should be used by developers wishing to implement groovy scripts
 * for PME.
 */
public abstract class BaseScript extends Script
{
    private final Logger logger = LoggerFactory.getLogger( BaseScript.class );

    private List<Project> projects;

    private Project project;

    private ProjectVersionRef gav;

    private File basedir;

    private Properties userProperties;

    private ModelIO modelIO;

    private MavenSessionHandler sessionHandler;

    private InvocationStage stage;

    /**
     * Return the current Project
     * @return a {@link org.commonjava.maven.ext.common.model.Project} instance.
     */
    public Project getProject()
    {
        return project;
    }

    /**
    * Returns the entire collection of Projects
    * @return an {@link java.util.ArrayList} of {@link org.commonjava.maven.ext.common.model.Project} instances.
    */
    public List<Project> getProjects()
    {
        return projects;
    }

    /**
     * Obtain the GAV of the current project
     * @return a {@link org.commonjava.maven.atlas.ident.ref.ProjectVersionRef}
     */
    public ProjectVersionRef getGAV()
    {
        return gav;
    }

    /**
     * Get the working directory (the execution root).
     * @return a {@link java.io.File} reference.
     */
    public File getBaseDir()
    {
        return basedir;
    }

    /**
     * Get the user properties
     * @return a {@link java.util.Properties} reference.
     */
    public Properties getUserProperties()
    {
        return userProperties;
    }

    /**
     * Get the modelIO instance for remote artifact resolving.
     * @return a {@link ModelIO} reference.
     */
    public ModelIO getModelIO()
    {
        return modelIO;
    }

    /**
     * Get the MavenSessionHandler instance
     * @return a {@link MavenSessionHandler} reference.
     */
    public MavenSessionHandler getSession()
    {
        return sessionHandler;
    }

    /**
     * Get the current stage
     * @return a {@link InvocationStage} reference.
     */
    public InvocationStage getInvocationStage()
    {
        return stage;
    }

    /**
     * Internal use only - the {@link InitialGroovyManipulator} uses this to
     * initialise the values
     * @param modelIO the modelIO instance.
     * @param session the Session instance.
     * @param projects ArrayList of Project instances
     * @param project Current project
     * @param stage the current InvocationStage of the groovy script
     */
    public void setValues( ModelIO modelIO, ManipulationSession session, List<Project> projects, Project project,
                           InvocationStage stage )
    {
        this.modelIO = modelIO;
        this.sessionHandler = session;
        this.projects = projects;
        this.project = project;
        this.gav = project.getKey();
        this.basedir = project.getPom().getParentFile();
        this.userProperties = session.getUserProperties();
        this.stage = stage;

        logger.info ("Injecting values. Project is " + project + " with basedir " + basedir + " and properties " + userProperties);
    }

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
            currentProject.getResolvedManagedDependencies( sessionHandler )
                          .entrySet().stream()
                          .filter( a -> a.getKey().asProjectRef().equals( groupArtifact ) && a.getValue().getVersion().contains( "$" ) )
                          .forEach( a -> {
                              logger.info( "Found managed artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( sessionHandler, currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
            currentProject.getResolvedDependencies( sessionHandler )
                          .entrySet().stream()
                          .filter( a -> a.getKey().asProjectRef().equals( groupArtifact ) && a.getValue().getVersion().contains( "$" ) )
                          .forEach( a -> {
                              logger.info( "Found artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( sessionHandler, currentProject.getInheritedList(), a.getValue().getVersion() ) );
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
            currentProject.getResolvedManagedDependencies( sessionHandler )
                          .entrySet().stream()
                          .filter( a -> a.getValue().getVersion().equals( "${" + propertyKey + "}" ) )
                          .forEach( a -> {
                              logger.info( "Found managed artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( sessionHandler, currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
            currentProject.getResolvedDependencies( sessionHandler )
                          .entrySet().stream()
                          .filter( a -> a.getValue().getVersion().equals( "${" + propertyKey + "}" ) )
                          .forEach( a -> {
                              logger.info( "Found artifact {} (original dependency {})", a.getKey(), a.getValue() );
                              a.getValue().setVersion(
                                              PropertyResolver.resolvePropertiesUnchecked( sessionHandler, currentProject.getInheritedList(), a.getValue().getVersion() ) );
                          } );
        }
        catch (ManipulationUncheckedException e)
        {
            throw (ManipulationException)e.getCause();
        }
    }
}
