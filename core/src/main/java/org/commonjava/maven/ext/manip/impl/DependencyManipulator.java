/**
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
package org.commonjava.maven.ext.manip.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.util.PropertiesUtils;
import org.commonjava.maven.ext.manip.util.WildcardMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.commonjava.maven.ext.manip.util.IdUtils.gav;

/**
 * {@link Manipulator} implementation that can alter dependency (and dependency management) sections in a project's pom file.
 * Configuration is stored in a {@link DependencyState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "project-dependency-manipulator" )
public class DependencyManipulator implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Used to store mappings of old property to new version for explicit overrides.
     */
    private final HashMap<String, String> explicitVersionPropertyUpdateMap = new HashMap<>();
    /**
     * Used to store mappings of old property to new version.
     */
    private final HashMap<String, String> versionPropertyUpdateMap = new HashMap<>();

    @Requirement
    private ModelIO effectiveModelBuilder;


    /**
     * Initialize the {@link DependencyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link Manipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new DependencyState( userProps ) );
    }

    /**
     * No prescanning required for BOM manipulation.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }
        return internalApplyChanges( projects, session, loadRemoteOverrides( state ) );
    }

    /**
     * This will load the remote overrides. It will first try to load any overrides that might have
     * been prepopulated by the REST scanner, failing that it will load from a remote POM file.
     *
     * @param state the dependency state
     * @return the loaded overrides
     * @throws ManipulationException
     */
    private Map<ArtifactRef, String> loadRemoteOverrides( final DependencyState state )
        throws ManipulationException
    {
        Map<ArtifactRef, String> overrides = state.getRemoteRESTOverrides();

        if ( overrides == null)
        {
            overrides = new LinkedHashMap<>();
            final List<ProjectVersionRef> gavs = state.getRemoteBOMDepMgmt();

            if ( gavs == null || gavs.isEmpty() )
            {
                return overrides;
            }

            final ListIterator<ProjectVersionRef> iter = gavs.listIterator( gavs.size() );
            // Iterate in reverse order so that the first GAV in the list overwrites the last
            while ( iter.hasPrevious() )
            {
                final ProjectVersionRef ref = iter.previous();
                overrides.putAll( effectiveModelBuilder.getRemoteDependencyVersionOverrides( ref ) );
            }
        }
        return overrides;
    }

    @Override
    public int getExecutionIndex()
    {
        return 40;
    }

    private Set<Project> internalApplyChanges( final List<Project> projects, final ManipulationSession session,
                                               Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );
        final Set<Project> result = new HashSet<>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if (!overrides.isEmpty() || !state.getDependencyExclusions().isEmpty())
            {
                apply( session, project, model, overrides );

                result.add( project );
            }
        }

        // If we've changed something now update any old properties with the new values.
        if (!result.isEmpty())
        {
            logger.info ("Iterating for standard overrides...");
            for ( final String key : versionPropertyUpdateMap.keySet() )
            {
                PropertiesUtils.PropertyUpdate found = PropertiesUtils.updateProperties( session, result, false, key, versionPropertyUpdateMap.get( key ) );

                if ( found == PropertiesUtils.PropertyUpdate.NOTFOUND )
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
            logger.info ("Iterating for explicit overrides...");
            for ( final String key : explicitVersionPropertyUpdateMap.keySet() )
            {
                PropertiesUtils.PropertyUpdate found = PropertiesUtils.updateProperties( session, result, true, key, explicitVersionPropertyUpdateMap.get( key ) );

                if ( found == PropertiesUtils.PropertyUpdate.NOTFOUND )
                {
                    // Problem in this scenario is that we know we have a property update map but we have not found a
                    // property to update. Its possible this property has been inherited from a parent. Override in the
                    // top pom for safety.
                    logger.info( "Unable to find a property for {} to update for explicit overrides", key );
                    for ( final Project p : result )
                    {
                        if ( p.isInheritanceRoot() )
                        {
                            logger.info( "Adding property {} with {} ", key, explicitVersionPropertyUpdateMap.get( key ) );
                            p.getModel().getProperties().setProperty( key, explicitVersionPropertyUpdateMap.get( key ) );
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Applies dependency overrides to the project.
     */
    private void apply( final ManipulationSession session, final Project project, final Model model,
                        final Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        // Map of Group : Map of artifactId [ may be wildcard ] : value
        final WildcardMap<String> explicitOverrides = new WildcardMap<>();
        final String projectGA = ga( project );
        final DependencyState state = session.getState( DependencyState.class );

        Map<ArtifactRef, String> moduleOverrides = new LinkedHashMap<>( overrides );
        moduleOverrides = removeReactorGAs( session, moduleOverrides );

        try
        {
            moduleOverrides = applyModuleVersionOverrides( projectGA,
                                                           state.getDependencyExclusions(),
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
                for ( ArtifactRef ar : moduleOverrides.keySet() )
                {
                    String oldValue = project.getParent().getVersion();
                    String newValue = moduleOverrides.get( ar );

                    if ( ar.asProjectRef().equals( SimpleProjectRef.parse( ga(project.getParent()) ) ))
                    {
                        if ( state.getStrict() )
                        {
                            if ( !PropertiesUtils.checkStrictValue( session, oldValue, newValue ) )
                            {
                                if ( state.getFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                    "Parent reference {} replacement: {} of original version: {} violates the strict version-alignment rule!",
                                                    ga( project.getParent() ), newValue, oldValue );
                                }
                                else
                                {
                                    logger.warn( "Parent reference {} replacement: {} of original version: {} violates the strict version-alignment rule!",
                                                 ga( project.getParent() ), newValue, oldValue );
                                    // Ignore the dependency override. As found has been set to true it won't inject
                                    // a new property either.
                                    continue;
                                }
                            }
                        }

                        logger.debug( " Modifying parent reference from {} to {} for {} ",
                                      model.getParent().getVersion(), newValue, ga( project.getParent() ) );
                        model.getParent().setVersion( newValue );
                        break;
                    }
                }

                // Apply any explicit overrides to the top level parent. Convert it to a simulated
                // dependency so we can reuse applyExplicitOverrides.
                ArrayList<Dependency> pDeps = new ArrayList<>();
                Dependency d = new Dependency();
                d.setGroupId( project.getParent().getGroupId() );
                d.setArtifactId( project.getParent().getArtifactId() );
                d.setVersion( project.getParent().getVersion() );
                pDeps.add( d );
                applyExplicitOverrides( explicitVersionPropertyUpdateMap, explicitOverrides, pDeps );
                project.getParent().setVersion( d.getVersion() );
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

                final Map<ArtifactRef, String> matchedOverrides = new LinkedHashMap<>( moduleOverrides );
                matchedOverrides.keySet().removeAll( nonMatchingVersionOverrides.keySet() );

                applyExplicitOverrides( explicitVersionPropertyUpdateMap, explicitOverrides, dependencies );

                if ( session.getState( DependencyState.class ).getOverrideTransitive() )
                {
                    final List<Dependency> extraDeps = new ArrayList<>();

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

                        final String artifactVersion = moduleOverrides.get( var );
                        newDependency.setVersion( artifactVersion );

                        extraDeps.add( newDependency );
                        logger.debug( "New entry added to <DependencyManagement/> - {} : {} ", var, artifactVersion );
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
                logger.debug( "NOT applying overrides to managed dependencies for top-pom: {}\n{}", projectGA,
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
                applyExplicitOverrides( explicitVersionPropertyUpdateMap, explicitOverrides,
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
            applyExplicitOverrides( explicitVersionPropertyUpdateMap, explicitOverrides, projectDependencies );

            // Now check all possible profiles and update them.
            List<Profile> profiles = project.getModel().getProfiles();
            if ( profiles != null )
            {
                for ( Profile p : profiles )
                {
                    logger.debug( "Iterating profile {} " , p.getId() );
                    if ( p.getDependencyManagement() != null )
                    {
                        applyOverrides( session, p.getDependencyManagement().getDependencies(), moduleOverrides );
                        applyExplicitOverrides( explicitVersionPropertyUpdateMap, explicitOverrides,
                                                p.getDependencyManagement().getDependencies() );
                    }
                    final List<Dependency> profileDependencies = p.getDependencies();
                    applyOverrides( session, profileDependencies, moduleOverrides );
                    applyExplicitOverrides( explicitVersionPropertyUpdateMap, explicitOverrides, profileDependencies );
                }
            }
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
     * @param versionPropertyUpdateMap properties to update
     * @param explicitOverrides
     * @param dependencies dependencies to check
     * @throws ManipulationException
     */
    private void applyExplicitOverrides( final Map<String, String> versionPropertyUpdateMap,
                                         final WildcardMap<String> explicitOverrides, final List<Dependency> dependencies )
                    throws ManipulationException
    {
        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            final ProjectRef groupIdArtifactId = new SimpleProjectRef( dependency.getGroupId(), dependency.getArtifactId() );

            if ( explicitOverrides.containsKey( groupIdArtifactId ) )
            {
                final String overrideVersion = explicitOverrides.get( groupIdArtifactId );
                final String oldVersion = dependency.getVersion();

                if ( isEmpty( overrideVersion )|| isEmpty( oldVersion ) )
                {
                    if ( isEmpty( oldVersion ) )
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
                    logger.info( "Explicit overrides : force aligning {} to {}.", groupIdArtifactId, overrideVersion );

                    if ( ! PropertiesUtils.cacheProperty( versionPropertyUpdateMap, oldVersion, overrideVersion, dependency, true ))
                    {
                        if ( oldVersion.contains( "${" ))
                        {
                            logger.warn( "Overriding version with {} when old version contained a property {} ", overrideVersion, oldVersion );
                            // TODO: Should this throw an exception?
                        }
                        // Not checking strict version alignment here as explicit overrides take priority.
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
        final Map<ArtifactRef, String> unmatchedVersionOverrides = new LinkedHashMap<>();
        unmatchedVersionOverrides.putAll( overrides );

        if ( dependencies == null )
        {
            return unmatchedVersionOverrides;
        }

        final DependencyState state = session.getState( DependencyState.class );
        final boolean strict = state.getStrict();

        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            ProjectRef depPr = new SimpleProjectRef( dependency.getGroupId(), dependency.getArtifactId() );

            // We might have junit:junit:3.8.2 and junit:junit:4.1 for differing override scenarios within the
            // overrides list. If strict mode alignment is enabled, using multiple overrides will work with
            // different modules. It is currently undefined what will happen if non-strict mode is enabled and
            // multiple versions are in the remote override list (be it from a bom or rest call). Actually, what
            // will most likely happen is last-wins.
            for ( final ArtifactRef ar : overrides.keySet() )
            {
                ProjectRef groupIdArtifactId = ar.asProjectRef();

                if ( depPr.equals( groupIdArtifactId ) )
                {
                    final String oldVersion = dependency.getVersion();
                    final String overrideVersion = overrides.get( ar );
                    final String resolvedValue = PropertiesUtils.resolveProperties( session.getProjects(), oldVersion);

                    if ( isEmpty( overrideVersion ) )
                    {
                        logger.warn( "Unable to align with an empty override version for " + groupIdArtifactId + "; ignoring" );
                    }
                    else if ( isEmpty( oldVersion ) )
                    {
                        logger.debug( "Dependency is a managed version for " + groupIdArtifactId + "; ignoring" );
                    }
                    // If we're doing strict matching with properties, then the original parts should match.
                    // i.e. assuming original resolved value is 1.2 and potential new value is 1.2.rebuild-1
                    // then this is fine to continue. If the original is 1.2 and potential new value is 1.3.rebuild-1
                    // then don't bother to attempt to cache the property as the strict check would fail.
                    // This extra check avoids an erroneous "Property replacement clash" error.

                    // Can't blindly compare resolvedValue [original] against ar as ar / overrideVersion is the new GAV. We don't
                    // have immediate access to the original property so the closest thats feasible is verify strict matching.

                    else if ( strict && oldVersion.contains( "$" ) &&
                                    ! PropertiesUtils.checkStrictValue( session, resolvedValue, overrideVersion) )
                    {
                        logger.debug ("Original fully resolved version {} of {} does not match override version {} -> {} so ignoring",
                                      resolvedValue, dependency, ar, overrideVersion);
                        if ( state.getFailOnStrictViolation() )
                        {
                            throw new ManipulationException(
                                            "Replacing original property version {} (fully resolved: {} ) with new version {} for {} violates the strict version-alignment rule!",
                                            dependency.getVersion(), resolvedValue, ar.getVersionString(), ar.asProjectRef().toString());
                        }
                        else
                        {
                            logger.warn( "Replacing original property version {} with new version {} for {} violates the strict version-alignment rule!",
                                         resolvedValue, overrideVersion, dependency.getVersion() );
                        }
                    }
                    else
                    {
                        if ( ! PropertiesUtils.cacheProperty( versionPropertyUpdateMap, oldVersion, overrideVersion, ar, false ))
                        {
                            if ( strict && ! PropertiesUtils.checkStrictValue( session, resolvedValue, overrideVersion) )
                            {
                                if ( state.getFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                     "Replacing original version {} in dependency {} with new version {} violates the strict version-alignment rule!",
                                                     oldVersion, groupIdArtifactId.toString(), overrideVersion );
                                }
                                else
                                {
                                    logger.warn( "Replacing original version {} in dependency {} with new version {} violates the strict version-alignment rule!",
                                                 oldVersion, groupIdArtifactId, overrideVersion );
                                }
                            }
                            else
                            {
                                logger.debug( "Altered dependency {} {} -> {}", groupIdArtifactId, oldVersion,
                                              overrideVersion );

                                if ( oldVersion.contains( "${" ) )
                                {
                                    String appendValue = StringUtils.removeStart( overrideVersion, resolvedValue );
                                    logger.debug ( "Resolved value is {} and appended is {} ", resolvedValue, appendValue );

                                    // In this case the previous value couldn't be cached even though it contained a property
                                    // as it was either multiple properties or a property combined with a hardcoded value. Therefore
                                    // just append the suffix.
                                    dependency.setVersion( oldVersion + appendValue );
                                }
                                else
                                {
                                    dependency.setVersion( overrideVersion );
                                }
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
        final Map<ArtifactRef, String> reducedVersionOverrides = new LinkedHashMap<>( versionOverrides );
        for ( final Project project : session.getProjects() )
        {
            final String reactorGA = gav( project.getModel() );
            reducedVersionOverrides.remove( SimpleArtifactRef.parse( reactorGA ) );
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
        final Map<ArtifactRef, String> remainingOverrides = new LinkedHashMap<>( originalOverrides );

        logger.debug( "Calculating module-specific version overrides. Starting with:\n  {}",
                      join( remainingOverrides.entrySet(), "\n  " ) );

        // These modes correspond to two different kinds of passes over the available override properties:
        // 1. Module-specific: Don't process wildcard overrides here, allow module-specific settings to take precedence.
        // 2. Wildcards: Add these IF there is no corresponding module-specific override.
        final boolean wildcardMode[] = { false, true };
        for ( boolean aWildcardMode : wildcardMode )
        {
            for ( final String currentKey : new HashSet<>( moduleOverrides.keySet() ) )
            {
                final String currentValue = moduleOverrides.get( currentKey );

                logger.debug( "Processing key {} for override with value {}", currentKey, currentValue );

                if ( !currentKey.contains( "@" ) )
                {
                    logger.debug( "Not an override. Skip." );
                    continue;
                }

                final boolean isWildcard = currentKey.endsWith( "@*" );
                logger.debug( "Is wildcard? {}", isWildcard );

                // process module-specific overrides (first)
                if ( !aWildcardMode )
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
                        if ( currentValue != null && !currentValue.isEmpty() )
                        {
                            explicitOverrides.put( SimpleProjectRef.parse( artifactGA ), currentValue );
                            logger.debug( "Overriding module dependency for {} with {} : {}", moduleGA, artifactGA,
                                          currentValue );
                        }
                        else
                        {
                            removeGA( remainingOverrides, SimpleProjectRef.parse( artifactGA ) );
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

                    if ( explicitOverrides.containsKey( SimpleProjectRef.parse( artifactGA ) ) )
                    {
                        logger.debug( "For artifact override: {}, current overrides already contain a module-specific version. Skip.",
                                      artifactGA );
                        continue;
                    }

                    // I think this is only used for e.g. dependencyExclusion.groupId:artifactId@*=<explicitVersion>
                    if ( currentValue != null && !currentValue.isEmpty() )
                    {
                        logger.debug( "Overriding module dependency for {} with {} : {}", projectGA, artifactGA,
                                      currentValue );
                        explicitOverrides.put( SimpleProjectRef.parse( artifactGA ), currentValue );
                    }
                    else
                    {
                        // If we have a wildcard artifact we want to replace any prior explicit overrides
                        // with this one i.e. this takes precedence.
                        if ( artifactGA.endsWith( ":*" ) )
                        {
                            final ProjectRef artifactGAPr = SimpleProjectRef.parse( artifactGA );
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
                            removeGA( remainingOverrides, SimpleProjectRef.parse( artifactGA ) );
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

}
