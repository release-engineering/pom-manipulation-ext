/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.commonjava.maven.ext.manip.impl;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.spi.RemoteDependenciesSPI;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.DependencyState.VersionPropertyFormat;
import org.commonjava.maven.ext.manip.state.State;
import org.commonjava.maven.ext.manip.util.WildcardMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.commonjava.maven.ext.manip.util.IdUtils.gav;
import static org.commonjava.maven.ext.manip.util.PropertiesUtils.getPropertiesByPrefix;

/**
 * Common functionality shared between dependency implementations.
 */
abstract public class CommonDependencyManipulation
                implements RemoteDependenciesSPI
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Used to store mappings of old property to new version.
     */
    protected final HashMap<String, String> versionPropertyUpdateMap = new HashMap<String, String>();

    public abstract Map<? extends ProjectRef, String> load( final State state, final ManipulationSession session )
                    throws ManipulationException;

    protected Set<Project> internalApplyChanges( final List<Project> projects, final ManipulationSession session,
                                                 Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );

        final Set<Project> result = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if ( overrides.size() > 0 )
            {
                apply( session, project, model, overrides );

                result.add( project );
            }
        }

        // If we've changed something now update any old properties with the new values.
        if ( result.size() > 0 )
        {
            for ( final String key : versionPropertyUpdateMap.keySet() )
            {
                boolean found = updateProperties( state, result, key, versionPropertyUpdateMap.get( key ) );

                if ( !found )
                {
                    // Problem in this scenario is that we know we have a property update map but we have not found a
                    // property to update. Its possible this property has been inherited from a parent. Override in the
                    // top pom for safety.
                    logger.info( "Unable to find a property for {} to update", key );
                    for ( final Project p : result )
                    {
                        if ( p.isInheritanceRoot() )
                        {
                            logger.info( "Adding property {} with {} ", key, versionPropertyUpdateMap.get( key ) );
                            p.getModel().getProperties().setProperty( key, versionPropertyUpdateMap.get( key ) );
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Recursively update properties.
     *
     * @param state the DependencyState
     * @param projects the current set of projects we are scanning.
     * @param key a key to look for.
     * @param newValue a value to look for.
     * @return true if changes were made.
     * @throws ManipulationException
     */
    private boolean updateProperties( DependencyState state, Set<Project> projects, String key, String newValue )
                    throws ManipulationException
    {
        boolean found = false;
        for ( final Project p : projects )
        {
            if ( p.getModel().getProperties().containsKey( key ) )
            {
                final String oldValue = p.getModel().getProperties().getProperty( key );

                logger.info( "Updating property {} / {} with {} ", key, oldValue, newValue );

                found = true;

                if ( oldValue != null && oldValue.startsWith( "${" ) )
                {
                    if ( !updateProperties( state, projects, oldValue.substring( 2, oldValue.indexOf( '}' ) ),
                                            newValue ) )
                    {
                        logger.error( "Recursive property not found for {} with {} ", oldValue, newValue );
                        return false;
                    }
                }
                else
                {
                    if ( state.getStrict() )
                    {
                        if ( oldValue != null && !newValue.startsWith( oldValue ) )
                        {
                            if ( state.getFailOnStrictViolation() )
                            {
                                throw new ManipulationException(
                                                "Replacement: {} of original version: {} in property: {} violates the strict version-alignment rule!",
                                                newValue, oldValue, key );
                            }
                            else
                            {
                                logger.warn( "Replacement: {} of original version: {} in property: {} violates the strict version-alignment rule!",
                                             newValue, oldValue, key );
                                // Ignore the dependency override. As found has been set to true it won't inject
                                // a new property either.
                                continue;
                            }
                        }
                    }

                    p.getModel().getProperties().setProperty( key, newValue );
                }
            }
        }
        return found;
    }

    /**
     * Applies dependency overrides to the project.
     */
    private void apply( final ManipulationSession session, final Project project, final Model model,
                        final Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        // Map of Group : Map of artifactId [ may be wildcard ] : value
        final WildcardMap explicitOverrides = new WildcardMap();
        final String projectGA = ga( project );
        final DependencyState state = session.getState( DependencyState.class );

        Map<ArtifactRef, String> moduleOverrides = new LinkedHashMap<ArtifactRef, String>( overrides );
        moduleOverrides = removeReactorGAs( session, moduleOverrides );

        try
        {
            moduleOverrides = applyModuleVersionOverrides( projectGA,
                                                           getPropertiesByPrefix( session.getUserProperties(),
                                                                                  DependencyState.DEPENDENCY_EXCLUSION_PREFIX ),
                                                           moduleOverrides, explicitOverrides );
        }
        catch ( InvalidRefException e )
        {
            logger.error( "Invalid module exclusion override {} : {} ", moduleOverrides, explicitOverrides );
            throw e;
        }
        if ( project.isInheritanceRoot() )
        {
            // Handle the situation where the top level parent refers to a prior build that is in the BOM.
            if ( project.getParent() != null)
            {
                Iterator<ArtifactRef> it = moduleOverrides.keySet().iterator();
                while (it.hasNext())
                {
                    ArtifactRef ar = it.next();
                    String oldValue = project.getParent().getVersion();
                    String newValue = moduleOverrides.get( ar );

                    if ( ar.asProjectRef().equals( ProjectRef.parse( ga(project.getParent()) ) ))
                    {
                        if ( state.getStrict() )
                        {
                            if ( oldValue != null && !newValue.startsWith( oldValue ) )
                            {
                                if ( state.getFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                    "Parent reference {} replacement: {} of original version: {} violates the strict version-alignment rule!",
                                                    ga(project.getParent()), newValue, oldValue);
                                }
                                else
                                {
                                    logger.warn( "Parent reference {} replacement: {} of original version: {} violates the strict version-alignment rule!",
                                                 ga(project.getParent()), newValue, oldValue);
                                    // Ignore the dependency override. As found has been set to true it won't inject
                                    // a new property either.
                                    continue;
                                }
                            }
                        }

                        logger.debug( " Modifying parent reference from {} to {} for {} ", model.getParent().getVersion(),
                                      newValue, ga( project.getParent() ));
                        model.getParent().setVersion( newValue );
                        break;
                    }
                }
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

                logger.debug( "Applying overrides to managed dependencies for top-pom: {}\n{}", projectGA,
                              moduleOverrides );

                final Map<ArtifactRef, String> nonMatchingVersionOverrides =
                                applyOverrides( session, dependencies, moduleOverrides );

                final Map<ArtifactRef, String> matchedOverrides =
                                new LinkedHashMap<ArtifactRef, String>( moduleOverrides );
                matchedOverrides.keySet().removeAll( nonMatchingVersionOverrides.keySet() );

                applyExplicitOverrides( versionPropertyUpdateMap, explicitOverrides, dependencies );

                // Add/override a property to the build for each override
                addVersionOverrideProperties( session, matchedOverrides, model.getProperties() );

                if ( session.getState( DependencyState.class ).getOverrideTransitive() )
                {
                    final List<Dependency> extraDeps = new ArrayList<Dependency>();

                    // Add dependencies to Dependency Management which did not match any existing dependency
                    for ( final ArtifactRef var : overrides.keySet() )
                    {
                        if ( !nonMatchingVersionOverrides.containsKey( var ) )
                        {
                            // This one in the remote pom was already dealt with ; continue.
                            continue;
                        }

                        final Dependency newDependency = new Dependency();
                        newDependency.setGroupId( var.getGroupId() );
                        newDependency.setArtifactId( var.getArtifactId() );
                        newDependency.setType( var.getType() );
                        newDependency.setClassifier( var.getClassifier() );
                        if ( var.isOptional() )
                        {
                            newDependency.setOptional( var.isOptional() );
                        }

                        final String artifactVersion = moduleOverrides.get( var );
                        newDependency.setVersion( artifactVersion );

                        extraDeps.add( newDependency );
                        logger.debug( "New entry added to <DependencyManagement/> - {} : {} ", var, artifactVersion );

                        // Add/override a property to the build for each override
                        addVersionOverrideProperties( session, nonMatchingVersionOverrides, model.getProperties() );
                    }

                    dependencyManagement.getDependencies().addAll( 0, extraDeps );
                }
                else
                {
                    logger.debug( "Non-matching dependencies ignored." );
                }
            }
            else
            {
                logger.debug( "NOT applying overrides to managed dependencies for Top-pom: {}\n{}", projectGA,
                              moduleOverrides );
            }
        }
        else
        {
            // If a child module has a depMgmt section we'll change that as well.
            final DependencyManagement dependencyManagement = model.getDependencyManagement();
            if ( session.getState( DependencyState.class ).getOverrideDependencies() && dependencyManagement != null )
            {
                logger.debug( "Applying overrides to managed dependencies for: {}\n{}", projectGA, moduleOverrides );
                applyOverrides( session, dependencyManagement.getDependencies(), moduleOverrides );
                applyExplicitOverrides( versionPropertyUpdateMap, explicitOverrides,
                                        dependencyManagement.getDependencies() );
            }
            else
            {
                logger.debug( "NOT applying overrides to managed dependencies for: {}\n{}", projectGA,
                              moduleOverrides );
            }
        }

        if ( session.getState( DependencyState.class ).getOverrideDependencies() )
        {
            logger.debug( "Applying overrides to concrete dependencies for: {}\n{}", projectGA, moduleOverrides );
            // Apply overrides to project direct dependencies
            final List<Dependency> projectDependencies = model.getDependencies();
            applyOverrides( session, projectDependencies, moduleOverrides );
            applyExplicitOverrides( versionPropertyUpdateMap, explicitOverrides, projectDependencies );
        }
        else
        {
            logger.debug( "NOT applying overrides to concrete dependencies for: {}\n{}", projectGA, moduleOverrides );
        }
    }

    /**
     * Apply explicit overrides to a set of dependencies from a project. The explicit overrides come from
     * dependencyExclusion. However they have to be separated out from standard overrides so we can easily
     * ignore any property references (and overwrite them).
     *
     * @param explicitOverrides
     * @param dependencies
     * @throws ManipulationException
     */
    private void applyExplicitOverrides( final Map<String, String> versionPropertyUpdateMap,
                                         final WildcardMap explicitOverrides, final List<Dependency> dependencies )
                    throws ManipulationException
    {
        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            final ProjectRef groupIdArtifactId = new ProjectRef( dependency.getGroupId(), dependency.getArtifactId() );

            if ( explicitOverrides.containsKey( groupIdArtifactId ) )
            {
                final String overrideVersion = explicitOverrides.get( groupIdArtifactId );
                final String oldVersion = dependency.getVersion();

                if ( overrideVersion == null || overrideVersion.length() == 0 || oldVersion == null
                                || oldVersion.length() == 0 )
                {
                    if ( oldVersion == null || oldVersion.length() == 0 )
                    {
                        logger.warn( "Unable to force align as no existing version field to update for "
                                                     + groupIdArtifactId + "; ignoring" );
                    }
                    else
                    {
                        logger.warn( "Unable to force align as override version is empty for " + groupIdArtifactId
                                                     + "; ignoring" );
                    }
                }
                else
                {
                    logger.debug( "Force aligning {} to {}.", groupIdArtifactId, overrideVersion );

                    if ( oldVersion.startsWith( "${" ) )
                    {
                        final int endIndex = oldVersion.indexOf( '}' );
                        final String oldProperty = oldVersion.substring( 2, endIndex );

                        if ( endIndex != oldVersion.length() - 1 )
                        {
                            throw new ManipulationException( "NYI : handling for versions (" + oldVersion
                                                                             + ") with multiple embedded properties is NYI. " );
                        }
                        logger.debug( "Original version was a property mapping; caching new fixed value for update {} -> {}",
                                      oldProperty, overrideVersion );

                        final String oldVersionProp = oldVersion.substring( 2, oldVersion.length() - 1 );

                        versionPropertyUpdateMap.put( oldVersionProp, overrideVersion );
                    }
                    else
                    {
                        dependency.setVersion( overrideVersion );
                    }
                }
            }
        }
    }

    /**
     * Apply a set of version overrides to a list of dependencies. Return a set of the overrides which were not applied.
     *
     * @param session The ManipulationSession
     * @param dependencies The list of dependencies
     * @param overrides The map of dependency version overrides
     * @return The map of overrides that were not matched in the dependencies
     * @throws ManipulationException
     */
    private Map<ArtifactRef, String> applyOverrides( final ManipulationSession session, final List<Dependency> dependencies,
                                                     final Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        // Duplicate the override map so unused overrides can be easily recorded
        final Map<ArtifactRef, String> unmatchedVersionOverrides = new LinkedHashMap<ArtifactRef, String>();
        unmatchedVersionOverrides.putAll( overrides );

        if ( dependencies == null )
        {
            return unmatchedVersionOverrides;
        }

        final DependencyState state = session.getState( DependencyState.class );
        final boolean strict = state.getStrict();

        /*
        Theoretically there could be multiple GA with different V in the overrides list. This would only make
        sense in strict alignment mode - i.e. search out those deps with matching and change them.

        In the non-strict mode - we probably should not expect any multiples... convert all to GA format ?

        How also to account for failOnStrictViolation? This is for strict mode where we want to fail if they don't
        match.
         */

        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            ProjectRef depPr = new ProjectRef( dependency.getGroupId(), dependency.getArtifactId() );

            // We might have junit:junit:3.8.2 and junit:junit:4.1 for differing override scenarios within the
            // overrides list. If strict mode alignment is enabled, using multiple overrides will work with
            // different modules. It is currently undefined what will happen if non-strict mode is enabled and
            // multiple versions are in the remote override list (be it from a bom or rest call). Actually, what
            // will most likely happen is last-wins.
            for ( final ArtifactRef ar : overrides.keySet() )
            {
                ProjectRef groupIdArtifactId = ar.asProjectRef();
                logger.debug ("Comparing project group:artifact {} to overrides ga {} ", depPr, groupIdArtifactId);

                if ( depPr.equals( groupIdArtifactId ) )
                {
                    final String oldVersion = dependency.getVersion();
                    final String overrideVersion = overrides.get( ar );

                    if ( overrideVersion == null || overrideVersion.length() == 0 || oldVersion == null
                                    || oldVersion.length() == 0 )
                    {
                        logger.warn( "Unable to align to an empty version for " + groupIdArtifactId + "; ignoring" );
                    }
                    else
                    {
                        // Handle the situation where we are updating a dependency that has an existing property - in this
                        // case we want to update the property instead.
                        // TODO: Handle the scenario where the version might be ${....}${....}
                        if ( oldVersion.startsWith( "${" ) )
                        {
                            final int endIndex = oldVersion.indexOf( '}' );
                            final String oldProperty = oldVersion.substring( 2, endIndex );

                            if ( endIndex != oldVersion.length() - 1 )
                            {
                                throw new ManipulationException( "NYI : handling for versions (" + oldVersion
                                                                                 + ") with multiple embedded properties is NYI. " );
                            }
                            logger.debug( "Original version was a property mapping; caching new value for update {} -> {}",
                                          oldProperty, overrideVersion );

                            final String oldVersionProp = oldVersion.substring( 2, oldVersion.length() - 1 );

                            versionPropertyUpdateMap.put( oldVersionProp, overrideVersion );
                        }
                        else
                        {
                            // FIXME : Here we should be able to exact match if strict ...
                            if ( strict && !overrideVersion.startsWith( oldVersion ) )
                            {
                                if ( state.getFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                    "Replacement: {} of original version: {} in dependency: {} violates the strict version-alignment rule!",
                                                    overrideVersion, oldVersion, groupIdArtifactId.toString() );
                                }
                                else
                                {
                                    logger.warn( "Replacement: {} of original version: {} in dependency: {} violates the strict version-alignment rule!",
                                                 overrideVersion, oldVersion, groupIdArtifactId );
                                }
                            }
                            else
                            {
                                logger.debug( "Altered dependency {} {} -> {}", groupIdArtifactId, oldVersion,
                                              overrideVersion );
                                dependency.setVersion( overrideVersion );
                            }
                        }
                        unmatchedVersionOverrides.remove( ar );
                    }
                }
            }
        }

        return unmatchedVersionOverrides;
    }

    /**
     * Remove version overrides which refer to projects in the current reactor.
     * Projects in the reactor include things like inter-module dependencies
     * which should never be overridden.
     * @param session the ManipulationSession
     * @param versionOverrides current set of ArtifactRef:newVersion overrides.
     * @return A new Map with the reactor GAs removed.
     */
    private Map<ArtifactRef, String> removeReactorGAs( final ManipulationSession session,
                                                       final Map<ArtifactRef, String> versionOverrides )
    {
        final Map<ArtifactRef, String> reducedVersionOverrides =
                        new LinkedHashMap<ArtifactRef, String>( versionOverrides );
        for ( final Project project : session.getProjects() )
        {
            final String reactorGA = gav( project.getModel() );
            reducedVersionOverrides.remove( ArtifactRef.parse( reactorGA ) );
        }
        return reducedVersionOverrides;
    }

    /**
     * Remove module overrides which do not apply to the current module. Searches the full list of version overrides
     * for any keys which contain the '@' symbol.  Removes these from the version overrides list, and add them back
     * without the '@' symbol only if they apply to the current module.
     *
     * @param projectGA the current project group : artifact
     * @param originalOverrides The full list of version overrides, both global and module specific
     * @param moduleOverrides are individual overrides e.g. group:artifact@groupId:artifactId :: value
     * @param explicitOverrides
     * @return The map of global and module specific overrides which apply to the given module
     * @throws ManipulationException
     */
    private Map<ArtifactRef, String> applyModuleVersionOverrides( final String projectGA,
                                                                  final Map<String, String> moduleOverrides,
                                                                  Map<ArtifactRef, String> originalOverrides,
                                                                  final WildcardMap explicitOverrides )
                    throws ManipulationException
    {
        final Map<ArtifactRef, String> remainingOverrides = new LinkedHashMap<ArtifactRef, String>( originalOverrides );

        logger.debug( "Calculating module-specific version overrides. Starting with:\n  {}",
                      join( remainingOverrides.entrySet(), "\n  " ) );

        // These modes correspond to two different kinds of passes over the available override properties:
        // 1. Module-specific: Don't process wildcard overrides here, allow module-specific settings to take precedence.
        // 2. Wildcards: Add these IF there is no corresponding module-specific override.
        final boolean wildcardMode[] = { false, true };
        for ( int i = 0; i < wildcardMode.length; i++ )
        {
            for ( final String currentKey : new HashSet<String>( moduleOverrides.keySet() ) )
            {
                final String currentValue = moduleOverrides.get( currentKey );

                logger.debug( "Processing key {} for override with value {}", currentKey,  currentValue);

                if ( !currentKey.contains( "@" ) )
                {
                    logger.debug( "Not an override. Skip." );
                    continue;
                }

                final boolean isWildcard = currentKey.endsWith( "@*" );
                logger.debug( "Is wildcard? {}", isWildcard );

                // process module-specific overrides (first)
                if ( !wildcardMode[i] )
                {
                    // skip wildcard overrides in this mode
                    if ( isWildcard )
                    {
                        logger.debug( "Not currently in wildcard mode. Skip." );
                        continue;
                    }

                    final String[] artifactAndModule = currentKey.split( "@" );
                    if ( artifactAndModule.length != 2 )
                    {
                        throw new ManipulationException( "Invalid format for exclusion key " + currentKey );
                    }
                    final String artifactGA = artifactAndModule[0];
                    final String moduleGA = artifactAndModule[1];

                    logger.debug( "For artifact override: {}, comparing parsed module: {} to current project: {}",
                                  artifactGA, moduleGA, projectGA );

                    if ( moduleGA.equals( projectGA ) )
                    {
                        if ( currentValue != null && currentValue.length() > 0 )
                        {
                            explicitOverrides.put( ProjectRef.parse( artifactGA ), currentValue );
                            logger.debug( "Overriding module dependency for {} with {} : {}", moduleGA, artifactGA,
                                          currentValue );
                        }
                        else
                        {
                            removeGA( remainingOverrides, ProjectRef.parse( artifactGA ) );
                            logger.debug( "Ignoring module dependency override for {} " + moduleGA );
                        }
                    }
                }
                // process wildcard overrides (second)
                else
                {
                    // skip module-specific overrides in this mode
                    if ( !isWildcard )
                    {
                        logger.debug( "Currently in wildcard mode. Skip." );
                        continue;
                    }

                    final String artifactGA = currentKey.substring( 0, currentKey.length() - 2 );
                    logger.debug( "For artifact override: {}, checking if current overrides already contain a module-specific version.",
                                  artifactGA );

                    if ( explicitOverrides.containsKey( ProjectRef.parse( artifactGA ) ) )
                    {
                        logger.debug( "For artifact override: {}, current overrides already contain a module-specific version. Skip.",
                                      artifactGA );
                        continue;
                    }

                    // I think this is only used for e.g. dependencyExclusion.groupId:artifactId@*=<explicitVersion>
                    if ( currentValue != null && currentValue.length() > 0 )
                    {
                        logger.debug( "Overriding module dependency for {} with {} : {}", projectGA, artifactGA,
                                      currentValue );
                        explicitOverrides.put( ProjectRef.parse( artifactGA ), currentValue );
                    }
                    else
                    {
                        // If we have a wildcard artifact we want to replace any prior explicit overrides
                        // with this one i.e. this takes precedence.
                        if ( artifactGA.endsWith( ":*" ) )
                        {
                            final ProjectRef artifactGAPr = ProjectRef.parse( artifactGA );
                            final Iterator<ArtifactRef> it = remainingOverrides.keySet().iterator();
                            while ( it.hasNext() )
                            {
                                final ArtifactRef pr = it.next();
                                if ( artifactGAPr.getGroupId().equals( pr.getGroupId() ) )
                                {
                                    logger.debug( "Removing artifactGA " + pr + " from overrides" );
                                    it.remove();
                                }
                            }
                        }
                        else
                        {
                            removeGA( remainingOverrides, ProjectRef.parse( artifactGA ) );
                            logger.debug( "Removing artifactGA " + artifactGA + " from overrides" );
                        }
                        logger.debug( "Ignoring module dependency override for {} " + projectGA );
                    }
                }
            }
        }

        return remainingOverrides;
    }

    private void removeGA( Map<ArtifactRef, String> map, ProjectRef ref )
    {
        Iterator<ArtifactRef> it = map.keySet().iterator();

        while ( it.hasNext() )
        {
            ArtifactRef a = it.next();

            if ( a.asProjectRef().equals( ref ) )
            {
                it.remove();
            }
        }
    }

    /***
     * Add properties to the build which match the version overrides.
     * The property names are in the format
     * @param session the ManipulationSession
     */
    private void addVersionOverrideProperties( final ManipulationSession session,
                                               final Map<ArtifactRef, String> overrides, final Properties props )
    {
        final Properties properties = session.getUserProperties();
        VersionPropertyFormat result = VersionPropertyFormat.VG;

        switch ( VersionPropertyFormat.valueOf(
                        properties.getProperty( "versionPropertyFormat", VersionPropertyFormat.NONE.toString() )
                                  .toUpperCase() ) )
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
                // Property injection disabled.
                return;
            }
        }

        for ( final ArtifactRef currentGA : overrides.keySet() )
        {
            final String versionPropName = "version." + ( result == VersionPropertyFormat.VGA ?
                            currentGA.asProjectVersionRef().toString() :
                            currentGA.asProjectRef().toString() );

            logger.debug( "Adding version override property for {} of {}:{}", currentGA, versionPropName,
                          overrides.get( currentGA ) );
            props.setProperty( versionPropName, overrides.get( currentGA ) );
        }
    }
}
