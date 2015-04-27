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
/*
 * Copyright (c) 2010 Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see
 * <http://www.gnu.org/licenses>.
 */

package org.commonjava.maven.ext.manip.model;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingResult;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;

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

    public Project( final ProjectVersionRef key, final File pom, final Model model )
        throws ManipulationException
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

    public Project( final Model raw, final ModelBuildingResult mbResult, final File pom )
        throws ManipulationException
    {
        this.pom = pom;
        this.model = raw;
        this.key = modelKey( raw );
    }

    public File getPom()
    {
        return pom;
    }

    /**
     * Retrieve the model undergoing modification.
     * @return
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

    public String getGroupId()
    {
        return key.getGroupId();
    }

    public String getArtifactId()
    {
        return key.getArtifactId();
    }

    public String getId()
    {
        return model.getId();
    }

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
            return Collections.<String, Plugin> emptyMap();
        }

        final Map<String, Plugin> result = build.getPluginsAsMap();
        if ( result == null )
        {
            return Collections.<String, Plugin> emptyMap();
        }

        return result;
    }

    public Build getBuild()
    {
        return (Build) getBuild( model );
    }

    public BuildBase getBuild( final ModelBase base )
    {
        BuildBase build = null;
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

    public List<Plugin> getManagedPlugins()
    {
        return getManagedPlugins( model );
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

    public Map<String, Plugin> getManagedPluginMap()
    {
        return getManagedPluginMap( model );
    }

    public Map<String, Plugin> getManagedPluginMap( final ModelBase base )
    {
        if ( base instanceof Model )
        {
            final Build build = ( (Model) base ).getBuild();
            if ( build == null )
            {
                return Collections.<String, Plugin> emptyMap();
            }

            final PluginManagement pm = build.getPluginManagement();
            if ( pm == null )
            {
                return Collections.<String, Plugin> emptyMap();
            }

            final Map<String, Plugin> result = pm.getPluginsAsMap();
            if ( result == null )
            {
                return Collections.<String, Plugin> emptyMap();
            }

            return result;
        }

        return Collections.<String, Plugin> emptyMap();
    }

    public List<ReportPlugin> getReportPlugins()
    {
        return getReportPlugins( model );
    }

    public List<ReportPlugin> getReportPlugins( final ModelBase base )
    {
        final Reporting reporting = base.getReporting();
        if ( reporting == null )
        {
            return Collections.emptyList();
        }

        return reporting.getPlugins();
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

    /**
     * In the event the groupId or version changes in the model being modified
     * (represented by this Project instance), this method will update the stored
     * key with the new coordinate information.
     *
     * This can be important if the model doesn't specify a groupId and its
     * parent reference is relocated (which will result in this project's
     * groupId changing, since it's inherited under these circumstances).
     *
     * @throws ProjectToolsException If the new coordinate's version doesn't parse.
     */
    public void updateCoord()
    {
        key = modelKey( model );
    }

    /**
     * In cases where plugin configuration has been injected or removed, this
     * method will update the map of plugin keys to plugin instances within the
     * modified {@link Model} instance itself to reflect the changes.
     *
     * This may be necessary to make the updates available to other
     * {@link ProjectModder} instances that will run after the one making the
     * change.
     */
    public void flushPluginMaps()
    {
        flushPluginMaps( model );
        final List<Profile> profiles = model.getProfiles();
        if ( profiles != null )
        {
            for ( final Profile profile : profiles )
            {
                flushPluginMaps( profile );
            }
        }
    }

    public void flushPluginMaps( final ModelBase base )
    {
        final BuildBase build = getBuild( base );
        if ( build != null )
        {
            build.flushPluginMap();

            final PluginManagement pm = build.getPluginManagement();
            if ( pm != null )
            {
                pm.flushPluginMap();
            }
        }

        final Reporting reporting = model.getReporting();
        if ( reporting != null )
        {
            reporting.flushReportPluginMap();
        }
    }

    public ProjectRef getVersionlessParentKey()
    {
        final Parent parent = getParent();
        return parent != null ? new ProjectRef( parent.getGroupId(), parent.getArtifactId() ) : null;
    }

    public List<Extension> getExtensions()
    {
        if ( model.getBuild() == null )
        {
            return Collections.emptyList();
        }

        final List<Extension> extensions = model.getBuild()
                                                .getExtensions();
        if ( extensions == null )
        {
            return Collections.emptyList();
        }

        return extensions;
    }

    public String getPackaging()
    {
        return model.getPackaging();
    }

    public ProjectRef getVersionlessKey()
    {
        return key.asProjectRef();
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
    {
        String g = model.getGroupId();
        final String a = model.getArtifactId();
        String v = model.getVersion();

        if ( g == null || v == null )
        {
            final Parent p = model.getParent();
            if ( p == null )
            {
                throw new IllegalStateException( "Invalid model: " + model + " Cannot find groupId and/or version!" );
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


        return new ProjectVersionRef( g, a, v );
    }

}
