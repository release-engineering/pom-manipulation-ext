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
package org.commonjava.maven.ext.common.model;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.session.MavenSessionHandler;
import org.commonjava.maven.ext.common.util.ProfileUtils;
import org.commonjava.maven.ext.common.util.PropertyResolver;
import org.commonjava.maven.galley.maven.internal.defaults.StandardMaven304PluginDefaults;
import org.commonjava.maven.galley.maven.spi.defaults.MavenPluginDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.apache.commons.lang.StringUtils.isEmpty;
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
    private static final MavenPluginDefaults PLUGIN_DEFAULTS = new StandardMaven304PluginDefaults();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Original POM file from which this model information was loaded.
     */
    private final File pom;

    /**
     * Model undergoing modification during execution. This model is what
     * will eventually be written back to disk.
     */
    private final Model model;

    /**
     * Read-only copy of the original ProjectVersionRef. This is not updated when
     * the model is updated.
     */
    private final ProjectVersionRef key;

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

    /**
     * Returns the Project groupId. Uses the read-only ProjectVersionRef {@link #key}. Also used by Interpolator.
     */
    public String getGroupId()
    {
        return key.getGroupId();
    }

    /**
     * Returns the Project artifactId. Uses the read-only ProjectVersionRef {@link #key}. Also used by Interpolator.
     */
    public String getArtifactId()
    {
        return key.getArtifactId();
    }

    /**
     * Returns the Project version. Uses the read-only ProjectVersionRef {@link #key}. Also used by Interpolator.
     */
    public String getVersion()
    {
        return key.getVersionString();
    }

    /**
     * This method will scan the dependencies in the potentially active Profiles in this project and
     * return a fully resolved list.  Note that this will only return full dependencies not managed
     * i.e. those with a group, artifact and version.
     *
     * Note that while updating the {@link Dependency} reference returned will be reflected in the
     * Model as it is the same object, if you wish to remove or add items to the Model then you
     * must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ArtifactRef} to the original {@link Dependency}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<Profile, HashMap<ArtifactRef, Dependency>> getResolvedProfileDependencies( MavenSessionHandler session) throws ManipulationException
    {
        HashMap<Profile, HashMap<ArtifactRef, Dependency>> resolvedProfileDependencies = new HashMap<>();

        for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
        {
            HashMap<ArtifactRef, Dependency> profileDeps = new HashMap<>();

            resolveDeps( session, profile.getDependencies(), false, profileDeps );

            resolvedProfileDependencies.put( profile, profileDeps );
        }

        return resolvedProfileDependencies;
    }

    /**
     * This method will scan the dependencies in the potentially active Profiles in this project and
     * return a fully resolved list. Note that this will return all dependencies including managed
     * i.e. those with a group, artifact and potentially empty version.
     *
     * Note that while updating the {@link Dependency} reference returned will be reflected in the
     * Model as it is the same object, if you wish to remove or add items to the Model then you
     * must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ArtifactRef} to the original {@link Dependency}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<Profile, HashMap<ArtifactRef, Dependency>> getAllResolvedProfileDependencies( MavenSessionHandler session) throws ManipulationException
    {
        HashMap<Profile, HashMap<ArtifactRef, Dependency>> allResolvedProfileDependencies = new HashMap<>();

        for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
        {
            HashMap<ArtifactRef, Dependency> profileDeps = new HashMap<>();

            resolveDeps( session, profile.getDependencies(), true, profileDeps );

            allResolvedProfileDependencies.put( profile, profileDeps );
        }

        return allResolvedProfileDependencies;
    }

    /**
     * This method will scan the dependencies in the dependencyManagement section of the potentially active Profiles in
     * this project and return a fully resolved list. Note that while updating the {@link Dependency}
     * reference returned will be reflected in the Model as it is the same object, if you wish to remove or add items
     * to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ArtifactRef} to the original {@link Dependency} (that were within DependencyManagement)
     * @throws ManipulationException if an error occurs
     */
    public HashMap<Profile, HashMap<ArtifactRef, Dependency>> getResolvedProfileManagedDependencies( MavenSessionHandler session) throws ManipulationException
    {
        HashMap<Profile, HashMap<ArtifactRef, Dependency>> resolvedProfileManagedDependencies = new HashMap<>();

        for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
        {
            HashMap<ArtifactRef, Dependency> profileDeps = new HashMap<>();

            final DependencyManagement dm = profile.getDependencyManagement();
            if ( !( dm == null || dm.getDependencies() == null ) )
            {
                resolveDeps( session, dm.getDependencies(), false, profileDeps );

                resolvedProfileManagedDependencies.put( profile, profileDeps );
            }
        }
        return resolvedProfileManagedDependencies;
    }


    /**
     * This method will scan the plugins in this project and return a fully resolved list. Note that
     * while updating the {@link Plugin} reference returned will be reflected in the Model as it is the
     * same object, if you wish to remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Plugin}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<ProjectVersionRef, Plugin> getResolvedPlugins ( MavenSessionHandler session) throws ManipulationException
    {
        HashMap<ProjectVersionRef, Plugin> resolvedPlugins = new HashMap<>();

        if ( getModel().getBuild() != null )
        {
            resolvePlugins( session, getModel().getBuild().getPlugins(), resolvedPlugins );
        }

        return resolvedPlugins;
    }


    /**
     * This method will scan the plugins in the pluginManagement section of this project and return a fully
     * resolved list. Note that while updating the {@link Plugin} reference returned will be reflected in
     * the Model as it is the same object, if you wish to remove or add items to the Model then you must
     * use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Plugin}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<ProjectVersionRef, Plugin> getResolvedManagedPlugins ( MavenSessionHandler session) throws ManipulationException
    {
        HashMap<ProjectVersionRef, Plugin> resolvedManagedPlugins = new HashMap<>();

        if ( getModel().getBuild() != null )
        {
            final PluginManagement pm = getModel().getBuild().getPluginManagement();
            if ( !( pm == null || pm.getPlugins() == null ) )
            {
                resolvePlugins( session, pm.getPlugins(), resolvedManagedPlugins );
            }
        }

        return resolvedManagedPlugins;
    }

    /**
     * This method will scan the plugins in the potentially active Profiles in this project and
     * return a fully resolved list. Note that while updating the {@link Plugin} reference
     * returned will be reflected in the Model as it is the same object, if you wish to
     * remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Plugin}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<Profile,HashMap<ProjectVersionRef,Plugin>> getResolvedProfilePlugins( MavenSessionHandler session )
                    throws ManipulationException
    {
        HashMap<Profile, HashMap<ProjectVersionRef, Plugin>> resolvedProfilePlugins = new HashMap<>();

        for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
        {
            HashMap<ProjectVersionRef, Plugin> profileDeps = new HashMap<>();

            if ( profile.getBuild() != null )
            {
                resolvePlugins( session, profile.getBuild().getPlugins(), profileDeps );

            }
            resolvedProfilePlugins.put( profile, profileDeps );
        }

        return resolvedProfilePlugins;
    }

    /**
     * This method will scan the plugins in the pluginManagement section in the potentially active Profiles
     * in this project and return a fully resolved list. Note that while updating the {@link Plugin}
     * reference returned will be reflected in the Model as it is the same object, if you wish to remove
     * or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ProjectVersionRef} to the original {@link Plugin}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<Profile,HashMap<ProjectVersionRef,Plugin>> getResolvedProfileManagedPlugins( MavenSessionHandler session )
                    throws ManipulationException
    {
        HashMap<Profile, HashMap<ProjectVersionRef, Plugin>> resolvedProfileManagedPlugins = new HashMap<>();

        for ( final Profile profile : ProfileUtils.getProfiles( session, model ) )
        {
            HashMap<ProjectVersionRef, Plugin> profileDeps = new HashMap<>();

            if ( profile.getBuild() != null )
            {
                final PluginManagement pm = profile.getBuild().getPluginManagement();
                if ( !( pm == null || pm.getPlugins() == null ) )
                {
                    resolvePlugins( session, pm.getPlugins(), profileDeps );
                }
            }
            resolvedProfileManagedPlugins.put( profile, profileDeps );
        }
        return resolvedProfileManagedPlugins;
    }

    /**
     * This method will scan the dependencies in this project and return a fully resolved list. Note that this
     * will only return full dependencies not managed i.e. those with a group, artifact and version.
     *
     * Note that while updating the {@link Dependency} reference returned will be reflected in the Model
     * as it is the same object, if you wish to remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ArtifactRef} to the original {@link Dependency}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<ArtifactRef, Dependency> getResolvedDependencies( MavenSessionHandler session) throws ManipulationException
    {
        HashMap<ArtifactRef, Dependency> resolvedDependencies = new HashMap<>();

        resolveDeps( session, getModel().getDependencies(), false, resolvedDependencies );

        return resolvedDependencies;
    }


    /**
     * This method will scan the dependencies in this project and return a fully resolved list. Note that this
     * will return all dependencies including managed i.e. those with a group, artifact and potentially empty
     * version.
     *
     * Note that while updating the {@link Dependency} reference returned will be reflected in the Model
     * as it is the same object, if you wish to remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ArtifactRef} to the original {@link Dependency}
     * @throws ManipulationException if an error occurs
     */
    public HashMap<ArtifactRef, Dependency> getAllResolvedDependencies( MavenSessionHandler session ) throws ManipulationException
    {
        HashMap<ArtifactRef, Dependency> allResolvedDependencies = new HashMap<>();

        resolveDeps( session, getModel().getDependencies(), true, allResolvedDependencies );

        return allResolvedDependencies;
    }


    /**
     * This method will scan the dependencies in the dependencyManagement section of this project and return a
     * fully resolved list. Note that while updating the {@link Dependency} reference returned will be reflected
     * in the Model as it is the same object, if you wish to remove or add items to the Model then you must use {@link #getModel()}
     *
     * @param session MavenSessionHandler, used by {@link PropertyResolver}
     * @return a list of fully resolved {@link ArtifactRef} to the original {@link Dependency} (that were within DependencyManagement)
     * @throws ManipulationException if an error occurs
     */
    public HashMap<ArtifactRef, Dependency> getResolvedManagedDependencies( MavenSessionHandler session ) throws ManipulationException
    {
        HashMap<ArtifactRef, Dependency> resolvedManagedDependencies = new HashMap<>();

        final DependencyManagement dm = getModel().getDependencyManagement();
        if ( !( dm == null || dm.getDependencies() == null ) )
        {
            resolveDeps( session, dm.getDependencies(), false, resolvedManagedDependencies );
        }

        return resolvedManagedDependencies;
    }


    private void resolveDeps( MavenSessionHandler session, List<Dependency> deps, boolean includeManagedDependencies,
                              HashMap<ArtifactRef, Dependency> resolvedDependencies )
                    throws ManipulationException
    {
        ListIterator<Dependency> iterator = deps.listIterator( deps.size() );

        // Iterate in reverse order so later deps take precedence
        while ( iterator.hasPrevious() )
        {
            Dependency d = iterator.previous();

            String g = PropertyResolver.resolveInheritedProperties( session, this, "${project.groupId}".equals( d.getGroupId() ) ?
                            getGroupId() :
                            d.getGroupId() );
            String a = PropertyResolver.resolveInheritedProperties( session, this, "${project.artifactId}".equals( d.getArtifactId() ) ?
                            getArtifactId() :
                            d.getArtifactId() );
            String v = PropertyResolver.resolveInheritedProperties( session, this, d.getVersion() );

            if ( includeManagedDependencies && isEmpty( v ) )
            {
                v = "*";
            }
            if ( isNotEmpty( g ) && isNotEmpty( a ) && isNotEmpty( v ) )
            {
                SimpleArtifactRef sar = new SimpleArtifactRef( g, a, v, d.getType(), d.getClassifier() );

                // If the GAVTC already exists within the map it means we have a duplicate entry. While Maven
                // technically allows this it does warn that this leads to unstable models. In PME case this breaks
                // the indexing as we don't have duplicate entries. Given they are exact matches, remove older duplicate.
                if ( resolvedDependencies.containsKey( sar ) )
                {
                    logger.error( "Found duplicate entry within dependency list. Key of {} and dependency {}", sar, d );
                    iterator.remove();
                }
                else
                {
                    Dependency old = resolvedDependencies.put( sar, d );

                    if ( old != null )
                    {
                        logger.error( "Internal project dependency resolution failure ; replaced {} in store by {}:{}:{}.",
                                      old, g, a, v );
                        throw new ManipulationException(
                                        "Internal project dependency resolution failure ; replaced " + old + " by " + d );
                    }
                }
            }
        }
    }


    private void resolvePlugins ( MavenSessionHandler session, List<Plugin> plugins, HashMap<ProjectVersionRef, Plugin> resolvedPlugins)
                    throws ManipulationException
    {
        ListIterator<Plugin> iterator = plugins.listIterator( plugins.size() );

        // Iterate in reverse order so later plugins take precedence
        while ( iterator.hasPrevious() )
        {
            Plugin p = iterator.previous();

            String g = PropertyResolver.resolveInheritedProperties( session, this, "${project.groupId}".equals( p.getGroupId() ) ?
                            getGroupId() :
                            p.getGroupId() );
            String a = PropertyResolver.resolveInheritedProperties( session, this, "${project.artifactId}".equals( p.getArtifactId() ) ?
                            getArtifactId() :
                            p.getArtifactId() );
            String v = PropertyResolver.resolveInheritedProperties( session, this, p.getVersion() );

            // Its possible the internal plugin list is either abbreviated or empty. Attempt to fill in default values for
            // comparison purposes.
            if ( isEmpty( g ) )
            {
                g = PLUGIN_DEFAULTS.getDefaultGroupId( a );
            }
            // Theoretically we could default an empty v via PLUGIN_DEFAULTS.getDefaultVersion( g, a ) but
            // this means managed plugins would be included which confuses things.
            if ( isNotEmpty( g ) && isNotEmpty( a ) && isNotEmpty( v ) )
            {
                SimpleProjectVersionRef spv = new SimpleProjectVersionRef( g, a, v );

                // If the GAV already exists within the map it means we have a duplicate entry. While Maven
                // technically allows this it does warn that this leads to unstable models. In PME case this breaks
                // the indexing as we don't have duplicate entries. Given they are exact matches, remove older duplicate.
                if ( resolvedPlugins.containsKey( spv ) )
                {
                    logger.error( "Found duplicate entry within plugin list. Key of {} and plugin {}", spv, p );
                    iterator.remove();
                }
                else
                {
                    Plugin old = resolvedPlugins.put( spv, p );

                    if ( old != null )
                    {
                        logger.error( "Internal project plugin resolution failure ; replaced {} in store by {}.", old,
                                      spv );
                        throw new ManipulationException(
                                        "Internal project plugin resolution failure ; replaced " + old + " by " + spv );
                    }
                }
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

    public void updateProfiles (List<Profile> remoteProfiles)
    {
        final List<Profile> profiles = model.getProfiles();

        if ( !remoteProfiles.isEmpty() )
        {
            for ( Profile profile : remoteProfiles )
            {
                final Iterator<Profile> i = profiles.iterator();
                while ( i.hasNext() )
                {
                    final Profile p = i.next();

                    if ( profile.getId().equals( p.getId() ) )
                    {
                        logger.debug( "Removing local profile {} ", p );
                        i.remove();
                        // Don't break out of the loop so we can check for active profiles
                    }

                    // If we have injected profiles and one of the current profiles is using
                    // activeByDefault it will get mistakingly deactivated due to the semantics
                    // of activeByDefault. Therefore replace the activation.
                    if ( p.getActivation() != null && p.getActivation().isActiveByDefault() )
                    {
                        logger.warn( "Profile {} is activeByDefault", p );

                        final Activation replacement = new Activation();
                        final ActivationProperty replacementProp = new ActivationProperty();
                        replacementProp.setName( "!disableProfileActivation" );
                        replacement.setProperty( replacementProp );

                        p.setActivation( replacement );
                    }
                }

                logger.debug( "Adding profile {}", profile );
                profiles.add( profile );
            }
        }
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
}

