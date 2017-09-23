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
import org.commonjava.maven.ext.manip.session.MavenSessionHandler;
import org.commonjava.maven.ext.manip.util.ProfileUtils;
import org.commonjava.maven.ext.manip.util.PropertyResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

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

    private boolean incrementalPME;

    /**
     * Tracking inheritance across the project.
     */
    private Project projectParent;

    private HashMap<ProjectVersionRef, Dependency> resolvedDependencies;

    private HashMap<ProjectVersionRef, Dependency> resolvedManagedDepencies;

    private HashMap<Profile, HashMap<ProjectVersionRef, Dependency>> resolvedProfileDependencies;

    private HashMap<Profile, HashMap<ProjectVersionRef, Dependency>> resolvedProfileManagedDependencies;

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

    public Parent getModelParent()
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

    /**
     * This method will scan the dependencies in the potentially active Profiles in this project and
     * return a fully resolved list. Note that while updating the {@link Dependency} reference returned
     * will be reflected in the Model as it is the same object, if you wish to remove or add items to
     * the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Dependency}
     * @throws ManipulationException
     */
    public HashMap<Profile, HashMap<ProjectVersionRef, Dependency>> getResolvedProfileDependencies( MavenSessionHandler session) throws ManipulationException
    {
        if ( resolvedProfileDependencies == null )
        {
            resolvedProfileDependencies = new HashMap<>(  );

            for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
            {
                HashMap<ProjectVersionRef, Dependency> profileDeps = new HashMap<>();

                resolveDeps( session, profile.getDependencies(), profileDeps );

                resolvedProfileDependencies.put( profile, profileDeps );
            }
        }
        return resolvedProfileDependencies;
    }

    /**
     * This method will scan the dependencies in the dependencyManagement section of the potentially active Profiles in
     * this project and return a fully resolved list. Note that while updating the {@link Dependency}
     * reference returned will be reflected in the Model as it is the same object, if you wish to remove or add items
     * to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Dependency} (that were within DependencyManagement)
     * @throws ManipulationException
     */
    public HashMap<Profile, HashMap<ProjectVersionRef, Dependency>> getResolvedProfileManagedDependencies( MavenSessionHandler session) throws ManipulationException
    {
        if ( resolvedProfileManagedDependencies == null )
        {
            resolvedProfileManagedDependencies = new HashMap<>(  );

            for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
            {
                HashMap<ProjectVersionRef, Dependency> profileDeps = new HashMap<>();

                final DependencyManagement dm = profile.getDependencyManagement();
                if ( ! ( dm == null || dm.getDependencies() == null ) )
                {
                    resolveDeps( session, dm.getDependencies(), profileDeps );

                    resolvedProfileManagedDependencies.put( profile, profileDeps );
                }
            }
        }
        return resolvedProfileManagedDependencies;
    }

    /**
     * This method will scan the dependencies in this project and return a fully resolved list. Note that
     * while updating the {@link Dependency} reference returned will be reflected in the Model as it is the
     * same object, if you wish to remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Dependency}
     * @throws ManipulationException
     */
    public HashMap<ProjectVersionRef, Dependency> getResolvedDependencies( MavenSessionHandler session) throws ManipulationException
    {
        if ( resolvedDependencies == null )
        {
            resolvedDependencies = new HashMap<>();

            resolveDeps( session, getModel().getDependencies(), resolvedDependencies );
        }
        return resolvedDependencies;
    }


    /**
     * This method will scan the dependencies in the dependencyManagement section of this project and return a
     * fully resolved list. Note that while updating the {@link Dependency} reference returned will be reflected
     * in the Model as it is the same object, if you wish to remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Dependency} (that were within DependencyManagement)
     * @throws ManipulationException
     */
    public HashMap<ProjectVersionRef, Dependency> getResolvedManagedDependencies( MavenSessionHandler session ) throws ManipulationException
    {
        if ( resolvedManagedDepencies == null )
        {
            resolvedManagedDepencies = new HashMap<>(  );

            final DependencyManagement dm = getModel().getDependencyManagement();
            if ( ! ( dm == null || dm.getDependencies() == null ) )
            {
                resolveDeps( session, dm.getDependencies(), resolvedManagedDepencies );
            }
        }
        return resolvedManagedDepencies;
    }


    private void resolveDeps ( MavenSessionHandler session, List<Dependency> deps, HashMap<ProjectVersionRef, Dependency> resolvedDependencies)
                    throws ManipulationException
    {
        for ( Dependency d : deps )
        {
            String g = PropertyResolver.resolveInheritedProperties( session, this, "${project.groupId}".equals( d.getGroupId() ) ?
                            getGroupId() :
                            d.getGroupId() );
            String a = PropertyResolver.resolveInheritedProperties( session, this, "${project.artifactId}".equals( d.getArtifactId() ) ?
                            getArtifactId() :
                            d.getArtifactId() );
            String v = PropertyResolver.resolveInheritedProperties( session, this, "${project.version}".equals( d.getVersion() ) ?
                            d.getVersion() :
                            d.getVersion() );

            if ( isNotEmpty( g ) && isNotEmpty( a ) && isNotEmpty( v ) )
            {
                resolvedDependencies.put( new SimpleProjectVersionRef( g, a, v ), d );
            }
        }
    }

    public void setInheritanceRoot( final boolean inheritanceRoot )
    {
        this.inheritanceRoot = inheritanceRoot;
    }

    /**
     * @return true if this Project represents the top level POM of a build.
     */
    public boolean isInheritanceRoot()
    {
        return inheritanceRoot;
    }

    public static ProjectVersionRef modelKey( final Model model )
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

    public void setIncrementalPME( boolean incrementalPME )
    {
        this.incrementalPME = incrementalPME;
    }

    public boolean isIncrementalPME( )
    {
        return incrementalPME;
    }

    public void setProjectParent( Project parent )
    {
        this.projectParent = parent;
    }

    public Project getProjectParent()
    {
        return projectParent;
    }

    /**
     * @return inherited projects. Returned with order of root project first, down to this project.
     */
    public List<Project> getInheritedList()
    {
        final List<Project> found = new ArrayList<>(  );
        found.add( this );

        Project loop = this;
        while ( loop.getProjectParent() != null)
        {
            // Place inherited first so latter down tree take precedence.
            found.add( 0, loop.getProjectParent() );
            loop = loop.getProjectParent();
        }
        return found;
    }

    /**
     * @return inherited projects. Returned with order of this project first, up to root project.
     */
    public List<Project> getReverseInheritedList()
    {
        final List<Project> found = new ArrayList<>(  );
        found.add( this );

        Project loop = this;
        while ( loop.getProjectParent() != null)
        {
            // Place inherited last for iteration purposes
            found.add( loop.getProjectParent() );
            loop = loop.getProjectParent();
        }
        return found;
    }
}
