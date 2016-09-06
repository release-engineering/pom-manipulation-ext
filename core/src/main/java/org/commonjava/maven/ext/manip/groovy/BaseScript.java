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
package org.commonjava.maven.ext.manip.groovy;

import groovy.lang.Script;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.model.Project;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Abstract class that should be used by developers wishing to implement groovy scripts
 * for PME.
 */
public abstract class BaseScript extends Script
{
    protected List<Project> projects;

    protected Project project;

    protected ProjectVersionRef gav;

    protected File basedir;

    protected Properties userProperties;

    /**
     * Return the current Project
     * @return a {@link org.commonjava.maven.ext.manip.model.Project} instance.
     */
    public Project getProject()
    {
        return project;
    }

    /**
    * Returns the entire collection of Projects
    * @return an {@link java.util.ArrayList} of {@link org.commonjava.maven.ext.manip.model.Project} instances.
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
     * Internal use only - the {@link org.commonjava.maven.ext.manip.impl.GroovyManipulator} uses this to
     * initialise the values
     * @param userProperties User properties instance
     * @param projects ArrayList of Project instances
     * @param project Current project
     */
    public void setValues(Properties userProperties, List<Project> projects, Project project)
    {
        this.projects = projects;
        this.project = project;
        this.gav = project.getKey();
        this.basedir = project.getPom().getParentFile();
        this.userProperties = userProperties;

        System.out.println ("Injecting values. Project is " + project + " with basedir " + basedir + " and properties " + userProperties);
    }
}
