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
package org.commonjava.maven.ext.manip.model;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides a convenient way of passing around related information about a Maven
 * project without passing multiple parameters. The model in this class
 * represents the model that is being modified by the extension. Also stored is
 * the key and original POM file related to these models.
 *
 * @author jdcasey
 */
public class Project
{
    /**
     * Original POM file from which this model information was loaded.
     */
    private final File pom;

    /**
     * Model undergoing modification during execution. This model is what
     * will eventually be written back to disk.
     */
    private final Model model;

    private ProjectVersionRef key;

    /**
     * Denotes if this Project represents the top level POM of a build.
     */
    private boolean inheritanceRoot;

    /**
     * Denotes if this Project is the execution root.
     */
    private boolean executionRoot;

    public Project( final ProjectVersionRef key, final File pom, final Model model )
    {
        this.pom = pom;
        this.model = model;
        this.key = key;
    }

    public Project( final File pom, final Model model )
        throws ManipulationException
    {
        this( modelKey( model ), pom, model );
    }

    public Project( final Model model )
        throws ManipulationException
    {
        this( modelKey( model ), model.getPomFile(), model );
    }

    public File getPom()
    {
        return pom;
    }

    /**
     * Retrieve the model undergoing modification.
     * @return the Model being modified.
     */
    public Model getModel()
    {
        return model;
    }

    public ProjectVersionRef getKey()
    {
        return key;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final Project other = (Project) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return key + " [pom=" + pom + "]";
    }

    public Parent getParent()
    {
        return model.getParent();
    }

    // Used by Interpolator
    public String getGroupId()
    {
        return key.getGroupId();
    }

    // Used by Interpolator
    public String getArtifactId()
    {
        return key.getArtifactId();
    }

    public String getId()
    {
        return model.getId();
    }

    // Used by Interpolator
    public String getVersion()
    {
        return key.getVersionString();
    }

    public List<Plugin> getPlugins()
    {
        return getPlugins( model );
    }

    public List<Plugin> getPlugins( final ModelBase base )
    {
        final BuildBase build = getBuild( base );

        if ( build == null )
        {
            return Collections.emptyList();
        }

        final List<Plugin> result = build.getPlugins();
        if ( result == null )
        {
            return Collections.emptyList();
        }

        return result;
    }

    public Map<String, Plugin> getPluginMap()
    {
        return getPluginMap( model );
    }

    public Map<String, Plugin> getPluginMap( final ModelBase base )
    {
        final BuildBase build;
        if ( base instanceof Model )
        {
            build = ( (Model) base ).getBuild();
        }
        else
        {
            build = ( (Profile) base ).getBuild();
        }

        if ( build == null )
        {
            return Collections.emptyMap();
        }

        final Map<String, Plugin> result = build.getPluginsAsMap();
        if ( result == null )
        {
            return Collections.emptyMap();
        }

        return result;
    }

    public Build getBuild()
    {
        return (Build) getBuild( model );
    }

    public BuildBase getBuild( final ModelBase base )
    {
        BuildBase build;
        if ( base instanceof Model )
        {
            build = ( (Model) base ).getBuild();
        }
        else
        {
            build = ( (Profile) base ).getBuild();
        }

        return build;
    }

    public List<Plugin> getManagedPlugins( final ModelBase base )
    {
        BuildBase build;
        if ( base instanceof Model )
        {
            build = ( (Model) base ).getBuild();
        }
        else
        {
            build = ( (Profile) base ).getBuild();
        }

        if ( build == null )
        {
            return Collections.emptyList();
        }

        final PluginManagement pm = build.getPluginManagement();
        if ( pm == null )
        {
            return Collections.emptyList();
        }

        final List<Plugin> result = pm.getPlugins();
        if ( result == null )
        {
            return Collections.emptyList();
        }

        return result;
    }


    public Map<String, Plugin> getManagedPluginMap( final ModelBase base )
    {
        if ( base instanceof Model )
        {
            final Build build = ( (Model) base ).getBuild();
            if ( build == null )
            {
                return Collections.emptyMap();
            }

            final PluginManagement pm = build.getPluginManagement();
            if ( pm == null )
            {
                return Collections.emptyMap();
            }

            final Map<String, Plugin> result = pm.getPluginsAsMap();
            if ( result == null )
            {
                return Collections.emptyMap();
            }

            return result;
        }

        return Collections.emptyMap();
    }


    public Iterable<Dependency> getDependencies()
    {
        return getDependencies( model );
    }

    public Iterable<Dependency> getDependencies( final ModelBase base )
    {
        List<Dependency> deps = base.getDependencies();
        if ( deps == null )
        {
            deps = Collections.emptyList();
        }

        return deps;
    }

    public Iterable<Dependency> getManagedDependencies()
    {
        return getManagedDependencies( model );
    }

    public Iterable<Dependency> getManagedDependencies( final ModelBase base )
    {
        final DependencyManagement dm = base.getDependencyManagement();
        if ( dm == null || dm.getDependencies() == null )
        {
            return Collections.emptyList();
        }

        return dm.getDependencies();
    }

    public void setInheritanceRoot( final boolean inheritanceRoot )
    {
        this.inheritanceRoot = inheritanceRoot;
    }

    public boolean isInheritanceRoot()
    {
        return inheritanceRoot;
    }

    private static ProjectVersionRef modelKey( final Model model )
                    throws ManipulationException
    {
        String g = model.getGroupId();
        String v = model.getVersion();

        if ( g == null || v == null )
        {
            final Parent p = model.getParent();
            if ( p == null )
            {
                throw new ManipulationException( "Invalid model: " + model + " Cannot find groupId and/or version!" );
            }

            if ( g == null )
            {
                g = p.getGroupId();
            }
            if ( v == null )
            {
                v = p.getVersion();
            }

        }

        final String a = model.getArtifactId();
        return new SimpleProjectVersionRef( g, a, v );
    }

    public void setExecutionRoot()
    {
        executionRoot = true;
    }

    /**
     * Returns whether this project is the execution root.
     * @return true if this Project is the execution root.
     */
    public boolean isExecutionRoot()
    {
        return executionRoot;
    }
}
