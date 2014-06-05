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
package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.state.BOMState.GAV_SEPERATOR;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.BOMState;
import org.commonjava.maven.ext.manip.state.BOMState.VersionPropertyFormat;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

/**
 * {@link Manipulator} implementation that can alter dependency (and dependency management) sections in a project's pom file.
 * Configuration is stored in a {@link BOMState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "bom-manipulator" )
public class DependencyManipulator
    extends AlignmentManipulator
{
    protected DependencyManipulator()
    {
    }

    public DependencyManipulator( final ModelIO modelIO )
    {
        super( modelIO );
    }

    @Override
    public void init( final ManipulationSession session )
    {
        super.init( session );
    }

    @Override
    protected Map<String, String> loadRemoteBOM( final BOMState state, final ManipulationSession session )
        throws ManipulationException
    {
        return loadRemoteOverrides( RemoteType.DEPENDENCY, state.getRemoteDepMgmt(), session );
    }

    @Override
    protected void apply( final ManipulationSession session, final Project project, final Model model,
                          Map<String, String> override )
        throws ManipulationException
    {
        // TODO: Should plugin override apply to all projects?
        final String projectGA = ga( project );

        logger.info( "Applying dependency changes to: " + projectGA );

        override.putAll( BOMState.getPropertiesByPrefix( session.getUserProperties(),
                                                         BOMState.DEPENDENCY_EXCLUSION_PREFIX ) );

        override = removeReactorGAs( session, override );

        override = applyModuleVersionOverrides( projectGA, override );

        if ( project.isTopPOM() )
        {
            // Add/override a property to the build for each override
            addVersionOverrideProperties( session, override, model.getProperties() );

            // Handle the situation where the top level parent refers to a prior build that is in the BOM.
            if ( project.getParent() != null && override.containsKey( ga( project.getParent() ) ) )
            {
                model.getParent()
                     .setVersion( override.get( ga( project.getParent() ) ) );
            }

            // If the model doesn't have any Dependency Management set by default, create one for it
            DependencyManagement dependencyManagement = model.getDependencyManagement();
            if ( dependencyManagement == null )
            {
                dependencyManagement = new DependencyManagement();
                model.setDependencyManagement( dependencyManagement );
                logger.debug( "Added <DependencyManagement/> for current project" );
            }

            // Apply overrides to project dependency management
            final List<Dependency> dependencies = dependencyManagement.getDependencies();
            final Map<String, String> nonMatchingVersionOverrides = applyOverrides( dependencies, override );

            if ( overrideTransitive() )
            {
                // Add dependencies to Dependency Management which did not match any existing dependency
                for ( final String groupIdArtifactId : nonMatchingVersionOverrides.keySet() )
                {
                    final String[] groupIdArtifactIdParts = groupIdArtifactId.split( ":" );

                    if ( groupIdArtifactIdParts.length != 2 )
                    {
                        logger.error( "Invalid format for exclusion: " + groupIdArtifactId );
                        throw new ManipulationException( "Invalid format for exclusion: " + groupIdArtifactId );
                    }

                    final Dependency newDependency = new Dependency();
                    newDependency.setGroupId( groupIdArtifactIdParts[0] );
                    newDependency.setArtifactId( groupIdArtifactIdParts[1] );

                    final String artifactVersion = nonMatchingVersionOverrides.get( groupIdArtifactId );
                    newDependency.setVersion( artifactVersion );

                    dependencyManagement.getDependencies()
                                        .add( 0, newDependency );
                    logger.debug( "New entry added to <DependencyManagement/> - " + groupIdArtifactId + ":"
                        + artifactVersion );
                }
            }
            else
            {
                logger.debug( "Non-matching dependencies ignored." );
            }
        }
        else
        {
            // If a child module has a depMgmt section we'll change that as well.
            final DependencyManagement dependencyManagement = model.getDependencyManagement();
            if ( dependencyManagement != null )
            {
                applyOverrides( dependencyManagement.getDependencies(), override );
            }
        }

        // Apply overrides to project direct dependencies
        final List<Dependency> projectDependencies = model.getDependencies();
        applyOverrides( projectDependencies, override );
    }

    /**
     * Apply a set of version overrides to a list of dependencies. Return a set of the overrides which were not applied.
     *
     * @param dependencies The list of dependencies
     * @param overrides The map of dependency version overrides
     * @return The map of overrides that were not matched in the dependencies
     */
    private Map<String, String> applyOverrides( final List<Dependency> dependencies, final Map<String, String> overrides )
    {
        // Duplicate the override map so unused overrides can be easily recorded
        final Map<String, String> unmatchedVersionOverrides = new HashMap<String, String>();
        unmatchedVersionOverrides.putAll( overrides );

        if ( dependencies == null )
        {
            return unmatchedVersionOverrides;
        }

        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            final String groupIdArtifactId = dependency.getGroupId() + GAV_SEPERATOR + dependency.getArtifactId();
            if ( overrides.containsKey( groupIdArtifactId ) )
            {
                final String oldVersion = dependency.getVersion();
                final String overrideVersion = overrides.get( groupIdArtifactId );

                if ( overrideVersion == null || overrideVersion.length() == 0 || oldVersion == null
                    || oldVersion.length() == 0 )
                {
                    logger.warn( "Unable to align to an empty version for " + groupIdArtifactId + "; ignoring" );
                }
                else
                {
                    dependency.setVersion( overrideVersion );
                    logger.debug( "Altered dependency " + groupIdArtifactId + " " + oldVersion + "->" + overrideVersion );
                    unmatchedVersionOverrides.remove( groupIdArtifactId );
                }
            }
        }

        return unmatchedVersionOverrides;
    }

    /**
     * Remove version overrides which refer to projects in the current reactor.
     * Projects in the reactor include things like inter-module dependencies
     * which should never be overridden.
     * @param session
     *
     * @param versionOverrides
     * @return A new Map with the reactor GAs removed.
     */
    private Map<String, String> removeReactorGAs( final ManipulationSession session,
                                                  final Map<String, String> versionOverrides )
    {
        final Map<String, String> reducedVersionOverrides = new HashMap<String, String>( versionOverrides );
        for ( final Model model : session.getManipulatedModels()
                                         .values() )
        {
            final String reactorGA = ga( model );
            reducedVersionOverrides.remove( reactorGA );
        }
        return reducedVersionOverrides;
    }

    /**
     * Remove module overrides which do not apply to the current module. Searches the full list of version overrides
     * for any keys which contain the '@' symbol.  Removes these from the version overrides list, and add them back
     * without the '@' symbol only if they apply to the current module.
     *
     * @param versionOverides The full list of version overrides, both global and module specific
     * @return The map of global and module specific overrides which apply to the given module
     * @throws ManipulationException
     */
    private Map<String, String> applyModuleVersionOverrides( final String projectGA,
                                                             final Map<String, String> versionOverrides )
        throws ManipulationException
    {
        final Map<String, String> moduleVersionOverrides = new HashMap<String, String>( versionOverrides );
        for ( final String currentKey : versionOverrides.keySet() )
        {
            if ( currentKey.contains( "@" ) )
            {
                moduleVersionOverrides.remove( currentKey );
                final String[] artifactAndModule = currentKey.split( "@" );
                if ( artifactAndModule.length != 2 )
                {
                    throw new ManipulationException( "Invalid format for exclusion key " + currentKey );
                }
                final String artifactGA = artifactAndModule[0];
                final String moduleGA = artifactAndModule[1];
                if ( moduleGA.equals( projectGA ) || moduleGA.equals( "*" ) )
                {
                    if ( versionOverrides.get( currentKey ) != null && versionOverrides.get( currentKey )
                                                                                       .length() > 0 )
                    {
                        moduleVersionOverrides.put( artifactGA, versionOverrides.get( currentKey ) );
                        logger.debug( "Overriding module dependency for " + moduleGA + " with " + artifactGA + ':'
                            + versionOverrides.get( currentKey ) );
                    }
                    else
                    {
                        moduleVersionOverrides.remove( artifactGA );
                        logger.debug( "Ignoring module dependency override for " + moduleGA );
                    }
                }
            }
        }
        return moduleVersionOverrides;
    }

    /***
     * Add properties to the build which match the version overrides.
     * The property names are in the format
     * @param session
     */
    private void addVersionOverrideProperties( final ManipulationSession session, final Map<String, String> overrides,
                                               final Properties props )
    {
        final Properties properties = session.getUserProperties();
        VersionPropertyFormat result = VersionPropertyFormat.VG;

        switch ( VersionPropertyFormat.valueOf( properties.getProperty( "versionPropertyFormat",
                                                                        VersionPropertyFormat.VG.toString() ) ) )
        {
            case VG:
            {
                result = VersionPropertyFormat.VG;
                break;
            }
            case VGA:
            {
                result = VersionPropertyFormat.VGA;
                break;
            }
        }

        for ( final String currentGA : overrides.keySet() )
        {
            final String versionPropName =
                "version."
                    + ( result == VersionPropertyFormat.VGA ? currentGA.replace( ":", "." ) : currentGA.split( ":" )[0] );
            props.setProperty( versionPropName, overrides.get( currentGA ) );
        }
    }

    /**
     * Whether to override unmanaged transitive dependencies in the build. Has the effect of adding (or not) new entries
     * to dependency management when no matching dependency is found in the pom. Defaults to true.
     *
     * @return
     */
    private boolean overrideTransitive()
    {
        final String overrideTransitive = System.getProperties()
                                                .getProperty( "overrideTransitive", "true" );
        return overrideTransitive.equals( "true" );
    }
}
