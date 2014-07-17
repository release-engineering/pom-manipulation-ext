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

import static org.commonjava.maven.ext.manip.state.DependencyState.GAV_SEPERATOR;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.commonjava.maven.ext.manip.util.PropertiesUtils.getPropertiesByPrefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.DependencyState.VersionPropertyFormat;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.State;

/**
 * {@link Manipulator} implementation that can alter dependency (and dependency management) sections in a project's pom file.
 * Configuration is stored in a {@link DependencyState} instance, which is in turn stored in the {@link ManipulationSession}.
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

    /**
     * Initialize the {@link DependencyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link AlignmentManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new DependencyState( userProps ) );
    }

    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        return internalApplyChanges (session.getState( DependencyState.class ), projects, session);
    }

    @Override
    protected Map<ProjectRef, String> loadRemoteBOM( final State state, final ManipulationSession session )
        throws ManipulationException
    {
        return loadRemoteOverrides( RemoteType.DEPENDENCY, ((DependencyState)state).getRemoteDepMgmt(), session );
    }

    /**
     * Applies dependency overrides to the project.
     *
     * The overrides ProjectRef:version map has to be converted into Group|Artifact:Version map
     * for usage by exclusions.
     */
    @Override
    protected void apply( final ManipulationSession session, final Project project, final Model model,
                          final Map<ProjectRef, String> overrides )
        throws ManipulationException
    {
        final String projectGA = ga( project );

        // TODO: FIXME: Is it possible to avoid the secondary override map and just convert everything
        // to projectref's as required?

        // Convert into a Map of GA : version
        Map<String, String> override = new HashMap<String,String>();
        for (final ProjectRef var : overrides.keySet())
        {
            override.put( var.asProjectRef().toString(), overrides.get( var) );
        }
        override.putAll( getPropertiesByPrefix( session.getUserProperties(),
                                                         DependencyState.DEPENDENCY_EXCLUSION_PREFIX ) );
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

            if ( session.getState( DependencyState.class ).getOverrideDependencies() )
            {
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

                if ( session.getState( DependencyState.class ).getOverrideTransitive() )
                {
                    // Add dependencies to Dependency Management which did not match any existing dependency
                    for ( final ProjectRef projectRef : overrides.keySet() )
                    {
                        final VersionlessArtifactRef var = (VersionlessArtifactRef)projectRef;

                        if ( ! nonMatchingVersionOverrides.containsKey( var.asProjectRef().toString() ))
                        {
                            // This one in the remote pom was already dealt with ; continue.
                            continue;
                        }

                        final Dependency newDependency = new Dependency();
                        newDependency.setGroupId( var.getGroupId() );
                        newDependency.setArtifactId( var.getArtifactId() );
                        newDependency.setType( var.getType() );
                        newDependency.setClassifier( var.getClassifier() );
                        if (var.isOptional())
                        {
                            newDependency.setOptional( var.isOptional() );
                        }

                        final String artifactVersion = overrides.get( projectRef );

                        newDependency.setVersion( artifactVersion );

                        dependencyManagement.getDependencies()
                        .add( 0, newDependency );
                        logger.debug( "New entry added to <DependencyManagement/> - {} : {} ", projectRef, artifactVersion );
                    }
                }
                else
                {
                    logger.debug( "Non-matching dependencies ignored." );
                }
            }
        }
        else
        {
            // If a child module has a depMgmt section we'll change that as well.
            final DependencyManagement dependencyManagement = model.getDependencyManagement();
            if ( session.getState( DependencyState.class ).getOverrideDependencies() &&
                            dependencyManagement != null )
            {
                applyOverrides( dependencyManagement.getDependencies(), override );
            }
        }

        if (session.getState( DependencyState.class ).getOverrideDependencies() )
        {
            // Apply overrides to project direct dependencies
            final List<Dependency> projectDependencies = model.getDependencies();
            applyOverrides( projectDependencies, override );
        }
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
                    logger.debug( "Altered dependency {} {} -> {}", groupIdArtifactId, oldVersion, overrideVersion );
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
                        logger.debug( "Overriding module dependency for {} with {} : {}",
                                      moduleGA, artifactGA, versionOverrides.get( currentKey ) );
                    }
                    else
                    {
                        moduleVersionOverrides.remove( artifactGA );
                        logger.debug( "Ignoring module dependency override for {} " + moduleGA );
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
                                                                        VersionPropertyFormat.VG.toString() ).toUpperCase() ) )
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
            case NONE:
            {
                result = VersionPropertyFormat.NONE;
                // Property injection disabled.
                return;
            }
        }

        for ( final String currentGA : overrides.keySet() )
        {
            final String versionPropName =
                "version."
                    + ( result == VersionPropertyFormat.VGA ? currentGA.replace( ":", "." ) : currentGA.split( ":" )[0] );

            logger.debug( "Adding version override property for {} of {}:{}", currentGA, versionPropName, overrides.get( currentGA ));
            props.setProperty( versionPropName, overrides.get( currentGA ) );
        }
    }

}
