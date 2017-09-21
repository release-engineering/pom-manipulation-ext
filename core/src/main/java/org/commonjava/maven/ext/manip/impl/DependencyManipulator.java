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
package org.commonjava.maven.ext.manip.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
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
import org.commonjava.maven.ext.manip.state.CommonState;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.DependencyState.DependencyPrecedence;
import org.commonjava.maven.ext.manip.state.RESTState;
import org.commonjava.maven.ext.manip.util.ProfileUtils;
import org.commonjava.maven.ext.manip.util.PropertiesUtils;
import org.commonjava.maven.ext.manip.util.PropertyResolver;
import org.commonjava.maven.ext.manip.util.WildcardMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
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
    private final Map<Project,Map<String, String>> explicitVersionPropertyUpdateMap = new LinkedHashMap<>();

    /**
     * Used to store mappings of old property to new version.
     */
    private final Map<Project,Map<String, String>> versionPropertyUpdateMap = new LinkedHashMap<>();

    @Requirement
    private ModelIO effectiveModelBuilder;

    /**
     * Cache of modules we should ignore ; processed during applyModuleVersionOverrides and used
     * for whether to post-apply any property processing.
    private final HashSet<ProjectRef> ignoredModules = new HashSet<>();
     */

    private ManipulationSession session;

    /**
     * Initialize the {@link DependencyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link Manipulator#scan(List)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session ) throws ManipulationException
    {
        session.setState( new DependencyState( session.getUserProperties() ) );
        this.session = session;
    }

    /**
     * No prescanning required for BOM manipulation.
     */
    @Override
    public void scan( final List<Project> projects ) throws ManipulationException
    {
    }

    /**
     * Apply the alignment changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
                    throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }
        return internalApplyChanges( projects, loadRemoteOverrides() );
    }

    /**
     * This will load the remote overrides. It will first try to load any overrides that might have
     * been prepopulated by the REST scanner, failing that it will load from a remote POM file.
     *
     * @return the loaded overrides
     * @throws ManipulationException if an error occurs.
     */
    private Map<ArtifactRef, String> loadRemoteOverrides() throws ManipulationException
    {
        final DependencyState depState = session.getState( DependencyState.class );
        final RESTState restState = session.getState( RESTState.class );
        final List<ProjectVersionRef> gavs = depState.getRemoteBOMDepMgmt();

        // While in theory we are only mapping ProjectRef -> NewVersion if we store key as ProjectRef we can't then have
        // org.foo:foobar -> 1.2.0.redhat-2
        // org.foo:foobar -> 2.0.0.redhat-2
        // Which is useful for strictAlignment scenarios (although undefined for non-strict).
        Map<ArtifactRef, String> restOverrides = depState.getRemoteRESTOverrides();
        Map<ArtifactRef, String> bomOverrides = new LinkedHashMap<>();
        Map<ArtifactRef, String> mergedOverrides = new LinkedHashMap<>();

        if ( gavs != null && !gavs.isEmpty() )
        {
            final ListIterator<ProjectVersionRef> iter = gavs.listIterator( gavs.size() );
            // Iterate in reverse order so that the first GAV in the list overwrites the last
            while ( iter.hasPrevious() )
            {
                final ProjectVersionRef ref = iter.previous();
                Map<ArtifactRef, String> rBom = effectiveModelBuilder.getRemoteDependencyVersionOverrides( ref );

                // We don't normalise the BOM list here as ::applyOverrides can handle multiple GA with different V
                // for strict override. However, it is undefined if strict is not enabled.
                bomOverrides.putAll( rBom );
            }
        }

        if ( depState.getPrecedence() == DependencyPrecedence.BOM )
        {
            mergedOverrides = bomOverrides;
            if ( mergedOverrides.isEmpty() )
            {
                String msg = restState.isEnabled() ? "dependencySource for restURL" : "dependencyManagement";

                logger.warn( "No dependencies found for dependencySource {}. Has {} been configured? ", depState.getPrecedence(), msg );
            }
        }
        if ( depState.getPrecedence() == DependencyPrecedence.REST )
        {
            mergedOverrides = restOverrides;
            if ( mergedOverrides.isEmpty() )
            {
                logger.warn( "No dependencies found for dependencySource {}. Has restURL been configured? ", depState.getPrecedence() );
            }
        }
        else if ( depState.getPrecedence() == DependencyPrecedence.RESTBOM )
        {
            mergedOverrides = bomOverrides;

            removeDuplicateArtifacts( mergedOverrides, restOverrides );
            mergedOverrides.putAll( restOverrides );
        }
        else if ( depState.getPrecedence() == DependencyPrecedence.BOMREST )
        {
            mergedOverrides = restOverrides;
            removeDuplicateArtifacts( mergedOverrides, bomOverrides );
            mergedOverrides.putAll( bomOverrides );
        }
        logger.debug ("Final remote override list is {}", mergedOverrides);
        return mergedOverrides;
    }


    private void removeDuplicateArtifacts( Map<ArtifactRef, String> mergedOverrides, Map<ArtifactRef, String> targetOverrides )
    {
        Iterator<ArtifactRef> i = mergedOverrides.keySet().iterator();
        while ( i.hasNext() )
        {
            ArtifactRef key = i.next();
            ProjectRef pRef = key.asProjectRef();

            for ( ArtifactRef target : targetOverrides.keySet() )
            {
                if ( pRef.equals( target.asProjectRef() ) )
                {
                    logger.debug( "From source overrides artifact {} clashes with target {}", key, target );
                    i.remove();
                    break;
                }
            }
        }

    }

    @Override
    public int getExecutionIndex()
    {
        return 40;
    }

    private Set<Project> internalApplyChanges( final List<Project> projects, Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );
        final Set<Project> result = new HashSet<>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if (!overrides.isEmpty() || !state.getDependencyExclusions().isEmpty())
            {
                apply( project, model, overrides );

                result.add( project );
            }
        }

        // If we've changed something now update any old properties with the new values.
        if (!result.isEmpty())
        {
            logger.info ("Iterating for standard overrides...");
logger.info ("#### versionPropMap {} ", versionPropertyUpdateMap);
            for ( Project project : versionPropertyUpdateMap.keySet() )
            {
                logger.debug( "Checking property override within project {} ", project );
                for ( final Map.Entry<String, String> entry : versionPropertyUpdateMap.get( project ).entrySet() )
                {
                    PropertiesUtils.PropertyUpdate found =
                                    PropertiesUtils.updateProperties( session, project, false,
                                                                      entry.getKey(), entry.getValue() );

                    if ( found == PropertiesUtils.PropertyUpdate.NOTFOUND )
                    {
                        // Problem in this scenario is that we know we have a property update map but we have not found a
                        // property to update. Its possible this property has been inherited from a parent. Override in the
                        // top pom for safety.
                        logger.info( "Unable to find a property for {} to update", entry.getKey() );
                        logger.info( "Adding property {} with {} ", entry.getKey(), entry.getValue() );
                        // We know the inheritance root is at position 0 in the inherited list...
                        project.getInheritedList().get( 0 ).getModel().getProperties().setProperty( entry.getKey(), entry.getValue() );
                    }
                }
            }
            logger.info ("Iterating for explicit overrides...");

            for ( Project project : explicitVersionPropertyUpdateMap.keySet() )
            {
                logger.debug( "Checking property override within project {} ", project );
                for ( final Map.Entry<String, String> entry : explicitVersionPropertyUpdateMap.get( project ).entrySet() )
                {
                    PropertiesUtils.PropertyUpdate found =
                                    PropertiesUtils.updateProperties( session, project, true,
                                                                      entry.getKey(), entry.getValue() );

                    if ( found == PropertiesUtils.PropertyUpdate.NOTFOUND )
                    {
                        // Problem in this scenario is that we know we have a property update map but we have not found a
                        // property to update. Its possible this property has been inherited from a parent. Override in the
                        // top pom for safety.
                        logger.info( "Unable to find a property for {} to update for explicit overrides", entry.getKey() );
                        logger.info( "Adding property {} with {} ", entry.getKey(), entry.getValue() );
                        // We know the inheritance root is at position 0 in the inherited list...
                        project.getInheritedList().get( 0 ).getModel().getProperties().setProperty( entry.getKey(), entry.getValue() );
                    }
                }
            }
        }
        return result;
    }

    /**
     * Applies dependency overrides to the project.
     */
    private void apply( final Project project, final Model model, final Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        // Map of Group : Map of artifactId [ may be wildcard ] : value
        final WildcardMap<String> explicitOverrides = new WildcardMap<>();
        final String projectGA = ga( project );
        final DependencyState dependencyState = session.getState( DependencyState.class );
        final CommonState commonState = session.getState( CommonState.class );

        Map<ArtifactRef, String> moduleOverrides = new LinkedHashMap<>( overrides );
        moduleOverrides = removeReactorGAs( moduleOverrides );

        try
        {
            moduleOverrides = applyModuleVersionOverrides( projectGA,
                                                           dependencyState.getDependencyExclusions(),
                                                           moduleOverrides, explicitOverrides );
            logger.debug( "Module overrides are:\n{}", moduleOverrides );
            logger.debug( "Explicit overrides are:\n{}", explicitOverrides);
        }
        catch ( InvalidRefException e )
        {
            logger.error( "Invalid module exclusion override {} : {} ", moduleOverrides, explicitOverrides );
            throw e;
        }

        if ( project.isInheritanceRoot() )
        {
            // Handle the situation where the top level parent refers to a prior build that is in the BOM.
            if ( project.getModelParent() != null)
            {
                for ( Map.Entry<ArtifactRef, String> entry : moduleOverrides.entrySet() )
                {
                    String oldValue = project.getModelParent().getVersion();
                    String newValue = entry.getValue();

                    if ( entry.getKey().asProjectRef().equals( SimpleProjectRef.parse( ga(project.getModelParent()) ) ))
                    {
                        if ( dependencyState.getStrict() )
                        {
                            if ( !PropertiesUtils.checkStrictValue( session, oldValue, newValue ) )
                            {
                                if ( dependencyState.getFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                    "Parent reference {} replacement: {} of original version: {} violates the strict version-alignment rule!",
                                                    ga( project.getModelParent() ), newValue, oldValue );
                                }
                                else
                                {
                                    logger.warn( "Parent reference {} replacement: {} of original version: {} violates the strict version-alignment rule!",
                                                 ga( project.getModelParent() ), newValue, oldValue );
                                    // Ignore the dependency override. As found has been set to true it won't inject
                                    // a new property either.
                                    continue;
                                }
                            }
                        }

                        logger.debug( " Modifying parent reference from {} to {} for {} ",
                                      model.getParent().getVersion(), newValue, ga( project.getModelParent() ) );
                        model.getParent().setVersion( newValue );
                        break;
                    }
                }

                // Apply any explicit overrides to the top level parent. Convert it to a simulated
                // dependency so we can reuse applyExplicitOverrides.
                ArrayList<Dependency> pDeps = new ArrayList<>();
                Dependency d = new Dependency();
                d.setGroupId( project.getModelParent().getGroupId() );
                d.setArtifactId( project.getModelParent().getArtifactId() );
                d.setVersion( project.getModelParent().getVersion() );
                pDeps.add( d );
                applyExplicitOverrides( project, commonState, explicitVersionPropertyUpdateMap, explicitOverrides, pDeps );
                project.getModelParent().setVersion( d.getVersion() );
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

                logger.debug( "Applying overrides to managed dependencies for top-pom: {}", projectGA );

                final Map<ArtifactRef, String> nonMatchingVersionOverrides =
                                applyOverrides( project, dependencies, moduleOverrides, explicitOverrides );

                final Map<ArtifactRef, String> matchedOverrides = new LinkedHashMap<>( moduleOverrides );
                matchedOverrides.keySet().removeAll( nonMatchingVersionOverrides.keySet() );

                applyExplicitOverrides( project, commonState, explicitVersionPropertyUpdateMap, explicitOverrides, dependencies );

                if ( commonState.getOverrideTransitive() )
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
                logger.debug( "NOT applying overrides to managed dependencies for top-pom: {}", projectGA );
            }
        }
        else
        {
            // If a child module has a depMgmt section we'll change that as well.
            final DependencyManagement dependencyManagement = model.getDependencyManagement();
            if ( session.getState( DependencyState.class ).getOverrideDependencies() && dependencyManagement != null )
            {
                logger.debug( "Applying overrides to managed dependencies for: {}", projectGA );
                applyOverrides( project, dependencyManagement.getDependencies(), moduleOverrides, explicitOverrides );
                applyExplicitOverrides( project, commonState, explicitVersionPropertyUpdateMap, explicitOverrides,
                                        dependencyManagement.getDependencies() );
            }
            else
            {
                logger.debug( "NOT applying overrides to managed dependencies for: {}", projectGA );
            }
        }

        if ( session.getState( DependencyState.class ).getOverrideDependencies() )
        {
            logger.debug( "Applying overrides to concrete dependencies for: {}", projectGA );
            // Apply overrides to project direct dependencies
            final List<Dependency> projectDependencies = model.getDependencies();
            applyOverrides( project, projectDependencies, moduleOverrides, explicitOverrides );
            applyExplicitOverrides( project, commonState, explicitVersionPropertyUpdateMap, explicitOverrides, projectDependencies );

            // Now check all possible profiles and update them.
            List<Profile> profiles = ProfileUtils.getProfiles( session, project.getModel());
            if ( profiles != null )
            {
                for ( Profile p : profiles )
                {
                    if ( p.getDependencyManagement() != null )
                    {
                        applyOverrides( project, p.getDependencyManagement().getDependencies(), moduleOverrides,
                                        explicitOverrides );
                        applyExplicitOverrides( project, commonState, explicitVersionPropertyUpdateMap, explicitOverrides,
                                                p.getDependencyManagement().getDependencies() );
                    }
                    final List<Dependency> profileDependencies = p.getDependencies();
                    applyOverrides( project, profileDependencies, moduleOverrides, explicitOverrides );
                    applyExplicitOverrides( project, commonState, explicitVersionPropertyUpdateMap, explicitOverrides, profileDependencies );
                }
            }
        }
        else
        {
            logger.debug( "NOT applying overrides to concrete dependencies for: {}", projectGA );
        }
    }

    /**
     * Apply explicit overrides to a set of dependencies from a project. The explicit overrides come from
     * dependencyExclusion. However they have to be separated out from standard overrides so we can easily
     * ignore any property references (and overwrite them).
     *
     * @param project the current Project
     * @param state the CommonState, to retrieve Common Properties
     * @param versionPropertyUpdateMap properties to update
     * @param explicitOverrides a custom map to handle wildcard overrides
     * @param dependencies dependencies to check
     * @throws ManipulationException if an error occurs
     */
    private void applyExplicitOverrides( Project project, CommonState state, final Map<Project, Map<String, String>> versionPropertyUpdateMap,
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
                        logger.debug( "Unable to force align as no existing version field to update for "
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
                    for ( String target : overrideVersion.split( "," ) )
                    {
                        if (target.startsWith( "+" ))
                        {
                            logger.info ("Adding dependency exclusion {} to dependency {} ", target.substring( 1 ), dependency);
                            Exclusion e = new Exclusion();
                            e.setGroupId( target.substring( 1 ).split( ":" )[0] );
                            e.setArtifactId( target.split( ":" )[1] );
                            dependency.addExclusion( e );
                        }
                        else
                        {
                            logger.info( "Explicit overrides : force aligning {} to {}.", groupIdArtifactId,
                                         target );

                            if ( !PropertiesUtils.cacheProperty( project, state, versionPropertyUpdateMap, oldVersion, target,
                                                                 dependency, true ) )
                            {
                                if ( oldVersion.contains( "${" ) )
                                {
                                    logger.warn( "Overriding version with {} when old version contained a property {} ",
                                                 target, oldVersion );
                                    // TODO: Should this throw an exception?
                                }
                                // Not checking strict version alignment here as explicit overrides take priority.
                                dependency.setVersion( target );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Apply a set of version overrides to a list of dependencies. Return a set of the overrides which were not applied.
     *
     * @param project The current Project
     * @param dependencies The list of dependencies
     * @param overrides The map of dependency version overrides
     * @param explicitOverrides Any explicitOverrides to track for ignoring    @return The map of overrides that were not matched in the dependencies
     * @throws ManipulationException if an error occurs
     */
    private Map<ArtifactRef, String> applyOverrides( Project project, final List<Dependency> dependencies,
                                                     final Map<ArtifactRef, String> overrides,
                                                     WildcardMap<String> explicitOverrides )
                    throws ManipulationException
    {
        // Duplicate the override map so unused overrides can be easily recorded
        final Map<ArtifactRef, String> unmatchedVersionOverrides = new LinkedHashMap<>();
        unmatchedVersionOverrides.putAll( overrides );

        logger.debug ("### Applying to dpendencies {} ", dependencies);
        if ( dependencies == null )
        {
            return unmatchedVersionOverrides;
        }

        final DependencyState dependencyState = session.getState( DependencyState.class );
        final CommonState commonState= session.getState( CommonState.class );
        final boolean strict = dependencyState.getStrict();

        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            ProjectRef depPr = new SimpleProjectRef( dependency.getGroupId(), dependency.getArtifactId() );

            // We might have junit:junit:3.8.2 and junit:junit:4.1 for differing override scenarios within the
            // overrides list. If strict mode alignment is enabled, using multiple overrides will work with
            // different modules. It is currently undefined what will happen if non-strict mode is enabled and
            // multiple versions are in the remote override list (be it from a bom or rest call). Actually, what
            // will most likely happen is last-wins.
            for ( final Map.Entry<ArtifactRef, String> entry : overrides.entrySet() )
            {
                ProjectRef groupIdArtifactId = entry.getKey().asProjectRef();

                if ( depPr.equals( groupIdArtifactId ) )
                {
                    final String oldVersion = dependency.getVersion();
                    final String overrideVersion = entry.getValue();
                    final String resolvedValue = PropertyResolver.resolveInheritedProperties( session, project, oldVersion);

                    if ( isEmpty( overrideVersion ) )
                    {
                        logger.warn( "Unable to align with an empty override version for " + groupIdArtifactId + "; ignoring" );
                    }
                    else if ( isEmpty( oldVersion ) )
                    {
                        logger.debug( "Dependency is a managed version for " + groupIdArtifactId + "; ignoring" );
                    }
                    // If we have an explicitOverride, this will always override the dependency changes made here.
                    // By avoiding the potential duplicate work it also avoids a possible property clash problem.
                    else if ( explicitOverrides.containsKey( depPr ) )
                    {
                        logger.debug ("Dependency {} matches known explicit override so not performing initial override pass.", depPr);
                        unmatchedVersionOverrides.remove( entry.getKey() );
                    }
                    // If we're doing strict matching with properties, then the original parts should match.
                    // i.e. assuming original resolved value is 1.2 and potential new value is 1.2.rebuild-1
                    // then this is fine to continue. If the original is 1.2 and potential new value is 1.3.rebuild-1
                    // then don't bother to attempt to cache the property as the strict check would fail.
                    // This extra check avoids an erroneous "Property replacement clash" error.

                    // Can't blindly compare resolvedValue [original] against ar as ar / overrideVersion is the new GAV. We don't
                    // have immediate access to the original property so the closest that is feasible is verify strict matching.
                    else if ( strict && oldVersion.contains( "$" ) &&
                                    ! PropertiesUtils.checkStrictValue( session, resolvedValue, overrideVersion) )
                    {
                        logger.debug ("Original fully resolved version {} of {} does not match override version {} -> {} so ignoring",
                                      resolvedValue, dependency, entry.getKey(), overrideVersion);
                        if ( dependencyState.getFailOnStrictViolation() )
                        {
                            throw new ManipulationException(
                                            "For {} replacing original property version {} (fully resolved: {} ) with new version {} for {} violates the strict version-alignment rule!",
                                            depPr.toString(), dependency.getVersion(), resolvedValue, entry.getKey().getVersionString(), entry.getKey().asProjectRef().toString());
                        }
                        else
                        {
                            logger.warn( "Replacing original property version {} with new version {} for {} violates the strict version-alignment rule!",
                                         resolvedValue, overrideVersion, dependency.getVersion() );
                        }
                    }
                    else
                    {
                        // Too much spurious logging with project.version.
                        if ( ! oldVersion.equals( "${project.version}" ) )
                        {
                            logger.info( "Updating version {} for dependency {} from {}.", overrideVersion, dependency, project.getPom() );
                        }

                        if ( ! PropertiesUtils.cacheProperty( project, commonState, versionPropertyUpdateMap, oldVersion, overrideVersion, entry.getKey(), false ))
                        {
                            if ( oldVersion.equals( "${project.version}" ) )
                            {
                                logger.debug( "For dependency {} ; version is built in {} so skipping inlining {}", groupIdArtifactId, oldVersion,
                                              overrideVersion );
                            }
                            else if ( strict && ! PropertiesUtils.checkStrictValue( session, resolvedValue, overrideVersion) )
                            {
                                if ( dependencyState.getFailOnStrictViolation() )
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
                                logger.debug( "Altered dependency {} : {} -> {}", groupIdArtifactId, oldVersion,
                                              overrideVersion );

                                if ( oldVersion.contains( "${" ) )
                                {
                                    String suffix = PropertiesUtils.getSuffix( session );
                                    String replaceVersion;

                                    if ( dependencyState.getStrictIgnoreSuffix() && oldVersion.contains( suffix ) )
                                    {
                                        replaceVersion = StringUtils.substringBefore( oldVersion, suffix );
                                        replaceVersion += suffix + StringUtils.substringAfter( overrideVersion, suffix );
                                    }
                                    else
                                    {
                                        replaceVersion = oldVersion + StringUtils.removeStart( overrideVersion, resolvedValue );
                                    }
                                    logger.debug ( "Resolved value is {} and replacement version is {} ", resolvedValue, replaceVersion );

                                    // In this case the previous value couldn't be cached even though it contained a property
                                    // as it was either multiple properties or a property combined with a hardcoded value. Therefore
                                    // just append the suffix.
                                    dependency.setVersion( replaceVersion );
                                }
                                else
                                {
                                    dependency.setVersion( overrideVersion );
                                }
                            }
                        }
                        unmatchedVersionOverrides.remove( entry.getKey() );
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
     * @param versionOverrides current set of ArtifactRef:newVersion overrides.
     * @return A new Map with the reactor GAs removed.
     */
    private Map<ArtifactRef, String> removeReactorGAs( final Map<ArtifactRef, String> versionOverrides )
                    throws ManipulationException
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
     * @param explicitOverrides a custom map to handle wildcard overrides
     * @return The map of global and module specific overrides which apply to the given module
     * @throws ManipulationException if an error occurs
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

                final boolean isModuleWildcard = currentKey.endsWith( "@*" );
                logger.debug( "Is wildcard? {} and in module wildcard mode? {} ", isModuleWildcard, aWildcardMode );

                // process module-specific overrides (first)
                if ( !aWildcardMode )
                {
                    // skip wildcard overrides in this mode
                    if ( isModuleWildcard )
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
                    final ProjectRef moduleGA = SimpleProjectRef.parse( artifactAndModule[1] );

                    logger.debug( "For artifact override: {}, comparing parsed module: {} to current project: {}",
                                  artifactGA, moduleGA, projectGA );

                    if ( moduleGA.toString().equals( projectGA ) ||
                                    (
                                        moduleGA.getArtifactId().equals( "*" ) &&
                                        SimpleProjectRef.parse( projectGA ).getGroupId().equals( moduleGA.getGroupId()
                                    )
                        ) )
                    {
                        if ( currentValue != null && !currentValue.isEmpty() )
                        {
                            explicitOverrides.put( SimpleProjectRef.parse( artifactGA ), currentValue );
                            logger.debug( "Overriding module dependency for {} with {} : {}", moduleGA, artifactGA,
                                          currentValue );
                        }
                        else
                        {
                            // Override prevention...
                            removeGA( remainingOverrides, SimpleProjectRef.parse( artifactGA ) );
                            logger.debug( "For module {}, ignoring dependency override for {} ", moduleGA, artifactGA);
                        }
                    }
                }
                // process wildcard overrides (second)
                else
                {
                    // skip module-specific overrides in this mode i.e. format of groupdId:artifactId@*=
                    if ( !isModuleWildcard )
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
                        removeGA( remainingOverrides, SimpleProjectRef.parse( artifactGA ) );
                        logger.debug( "Removing artifactGA " + artifactGA + " from overrides" );
                    }
                }
            }
        }

        return remainingOverrides;
    }

    private void removeGA( Map<ArtifactRef, String> overrides, ProjectRef ref )
    {
        Iterator<ArtifactRef> it = overrides.keySet().iterator();

        while ( it.hasNext() )
        {
            ArtifactRef a = it.next();

            if ( a.asProjectRef().equals( ref ) ||
                ( ref.getArtifactId().equals( "*" ) && a.getGroupId().equals( ref.getGroupId() ) ) ||
                ( ref.getGroupId().equals( "*" ) && a.getArtifactId().equals( ref.getArtifactId() ) ) )
            {
                it.remove();
            }
            else if ( ref.getArtifactId().equals( "*" ) && ref.getGroupId().equals( "*" ) )
            {
                // For complete wildcard also cache the ignored module as we need the list later during
                // property processing.
                it.remove();
            }
        }
    }

}
