/*
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.commonjava.maven.ext.core.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.WildcardMap;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.CommonState;
import org.commonjava.maven.ext.core.state.DependencyState;
import org.commonjava.maven.ext.core.state.DependencyState.DependencyPrecedence;
import org.commonjava.maven.ext.core.state.RESTState;
import org.commonjava.maven.ext.core.util.PropertiesUtils;
import org.commonjava.maven.ext.core.util.PropertyMapper;
import org.commonjava.maven.ext.io.ModelIO;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.commonjava.maven.ext.core.util.IdUtils.ga;

/**
 * {@link Manipulator} implementation that can alter dependency (and dependency management) sections in a project's pom file.
 * Configuration is stored in a {@link DependencyState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Named("project-dependency-manipulator")
@Singleton
public class DependencyManipulator extends CommonManipulator implements Manipulator
{
    /**
     * Used to store mappings of old property to new version - the new version is encapsulated within the {@link PropertyMapper}
     * which also contains reference to the old version and the dependency that changed this. This allows complete tracking of
     * dependencies that updated properties - and therefore, the inverse, dependencies that did NOT update the property. This can
     * be problematic in the case of rebuilds.
     */
    private final Map<Project,Map<String, PropertyMapper>> versionPropertyUpdateMap = new LinkedHashMap<>();

    @Inject
    public DependencyManipulator(ModelIO effectiveModelBuilder)
    {
        this.effectiveModelBuilder = effectiveModelBuilder;
    }

    /**
     * Initialize the {@link DependencyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later.
     */
    @Override
    public void init( final ManipulationSession session ) throws ManipulationException
    {
        session.setState( new DependencyState( session.getUserProperties() ) );
        this.session = session;
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
            logger.debug("{}: Nothing to do!", getClass().getSimpleName());
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

        final Map<String, ProjectVersionRef> extraGAVs = depState.getExtraBOMs();
        final Map<String, Map<ProjectRef, String>> extraBOMOverrides = depState.getExtraBOMDepMgmts();

        // While in theory we are only mapping ProjectRef -> NewVersion if we store key as ProjectRef we can't then have
        // org.foo:foobar -> 1.2.0.redhat-2
        // org.foo:foobar -> 2.0.0.redhat-2
        // Which is useful for strictAlignment scenarios (although undefined for non-strict).
        Map<ArtifactRef, String> restOverrides = depState.getRemoteRESTOverrides();
        Map<ArtifactRef, String> bomOverrides = new LinkedHashMap<>();
        Map<ArtifactRef, String> mergedOverrides = new LinkedHashMap<>();

        logger.info ( "Remote precedence is {}", depState.getPrecedence() );
        if ( gavs != null )
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

        // Load extra BOMs into separate maps for accessing later, when applying the dependencyExclusions.
        for ( Map.Entry<String, ProjectVersionRef> entry : extraGAVs.entrySet() )
        {
            extraBOMOverrides.put( entry.getKey(),
                                   effectiveModelBuilder.getRemoteDependencyVersionOverridesByProject( entry.getValue() ) );
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
                    logger.debug( "Merging sources ; entry {}={} clashes (and will be removed) with precedence given to {}={}",
                                  key, mergedOverrides.get( key ), target, targetOverrides.get( target ) );
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
        final CommonState cState = session.getState( CommonState.class );
        final Set<Project> result = new HashSet<>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if (!overrides.isEmpty() || !state.getDependencyOverrides().isEmpty())
            {
                apply( project, model, overrides );

                result.add( project );
            }
        }

        // If we've changed something now update any old properties with the new values.
        if (!result.isEmpty())
        {
            if ( cState.getStrictDependencyPluginPropertyValidation() > 0 )
            {
                logger.info( "Iterating to validate dependency updates..." );
                for ( Project p : versionPropertyUpdateMap.keySet() )
                {
                    validateDependenciesUpdatedProperty( cState, p, p.getResolvedManagedDependencies( session ) );
                    validateDependenciesUpdatedProperty( cState, p, p.getResolvedDependencies( session ) );
                    for ( Profile profile : p.getResolvedProfileDependencies( session ).keySet() )
                    {
                        validateDependenciesUpdatedProperty( cState, p, p.getResolvedProfileDependencies( session ).get( profile ) );
                    }
                    for ( Profile profile : p.getResolvedProfileManagedDependencies( session ).keySet() )
                    {
                        validateDependenciesUpdatedProperty( cState, p, p.getResolvedProfileManagedDependencies( session ).get( profile ) );
                    }
                }
            }

            logger.info ("Iterating for property overrides...{}", versionPropertyUpdateMap);
            for ( Project project : versionPropertyUpdateMap.keySet() )
            {
                logger.debug( "Checking property override within project {} ", project );
                for ( final Map.Entry<String, PropertyMapper> entry : versionPropertyUpdateMap.get( project ).entrySet() )
                {
                    PropertiesUtils.PropertyUpdate found =
                                    PropertiesUtils.updateProperties( session, project, false,
                                                                      entry.getKey(), entry.getValue().getNewVersion() );

                    if ( found == PropertiesUtils.PropertyUpdate.NOTFOUND )
                    {
                        // Problem in this scenario is that we know we have a property update map but we have not found a
                        // property to update. Its possible this property has been inherited from a parent. Override in the
                        // top pom for safety.
                        logger.info( "Unable to find a property for {} to update", entry.getKey() );
                        logger.info( "Adding property {} with {} ", entry.getKey(), entry.getValue().getNewVersion() );
                        // We know the inheritance root is at position 0 in the inherited list...
                        project.getInheritedList()
                               .get( 0 )
                               .getModel()
                               .getProperties()
                               .setProperty( entry.getKey(), entry.getValue().getNewVersion() );
                    }
                }
            }

            explicitOverridePropertyUpdates(session);
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

        logger.debug( "Processing project {} ", projectGA );

        Map<ArtifactRef, String> originalOverrides = new LinkedHashMap<>( overrides );
        originalOverrides = removeReactorGAs( originalOverrides );

        try
        {
            originalOverrides = applyModuleVersionOverrides( projectGA, dependencyState.getDependencyOverrides(),
                                                           originalOverrides, explicitOverrides,
                                                           dependencyState.getExtraBOMDepMgmts() );
            logger.debug( "Module overrides are:{}{}", System.lineSeparator(), originalOverrides );
            logger.debug( "Explicit overrides are:{}{}", System.lineSeparator(), explicitOverrides );
        }
        catch ( InvalidRefException e )
        {
            logger.error( "Invalid module exclusion override {} : {} ", originalOverrides, explicitOverrides );
            throw e;
        }

        if ( project.isInheritanceRoot() )
        {
            // Handle the situation where the top level parent refers to a prior build that is in the BOM.
            if ( project.getModelParent() != null )
            {
                for ( Map.Entry<ArtifactRef, String> entry : originalOverrides.entrySet() )
                {
                    String oldValue = project.getModelParent().getVersion();
                    String newValue = entry.getValue();

                    if ( entry.getKey()
                              .asProjectRef()
                              .equals( SimpleProjectRef.parse( ga( project.getModelParent() ) ) ) )
                    {
                        if ( commonState.isStrict() )
                        {
                            if ( !PropertiesUtils.checkStrictValue( session, oldValue, newValue ) )
                            {
                                if ( commonState.isFailOnStrictViolation() )
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
                HashMap<ArtifactRef, Dependency> pDepMap = new HashMap<>();
                Dependency d = new Dependency();
                d.setGroupId( project.getModelParent().getGroupId() );
                d.setArtifactId( project.getModelParent().getArtifactId() );
                d.setVersion( project.getModelParent().getVersion() );
                pDepMap.put( SimpleArtifactRef.parse( d.getManagementKey() ), d );
                applyExplicitOverrides( project, pDepMap, explicitOverrides, commonState,
                                        explicitVersionPropertyUpdateMap );
                project.getModelParent().setVersion( d.getVersion() );
            }

            // Apply overrides to project dependency management
            logger.debug( "Applying overrides to managed dependencies for: {}", projectGA );

            final Map<ArtifactRef, String> nonMatchingVersionOverrides =
                            applyOverrides( project, project.getResolvedManagedDependencies( session ),
                                            explicitOverrides, originalOverrides );

            applyExplicitOverrides( project, project.getResolvedManagedDependencies( session ), explicitOverrides,
                                    commonState, explicitVersionPropertyUpdateMap );

            if ( commonState.isOverrideTransitive() )
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

                    final String artifactVersion = originalOverrides.get( var );
                    newDependency.setVersion( artifactVersion );

                    extraDeps.add( newDependency );
                    logger.debug( "New entry added to <DependencyManagement/> - {} : {} ", var, artifactVersion );
                }

                // If the model doesn't have any Dependency Management set by default, create one for it
                DependencyManagement dependencyManagement = model.getDependencyManagement();
                if ( extraDeps.size() > 0 )
                {
                    if ( dependencyManagement == null )
                    {
                        dependencyManagement = new DependencyManagement();
                        model.setDependencyManagement( dependencyManagement );
                        logger.debug( "Added <DependencyManagement/> for current project" );
                    }
                    dependencyManagement.getDependencies().addAll( 0, extraDeps );
                }
            }
            else
            {
                logger.debug( "Non-matching dependencies ignored." );
            }
        }
        else
        {
            logger.debug( "Applying overrides to managed dependencies for: {}", projectGA );
            applyOverrides( project, project.getResolvedManagedDependencies( session ), explicitOverrides,
                            originalOverrides );
            applyExplicitOverrides( project, project.getResolvedManagedDependencies( session ), explicitOverrides,
                                    commonState, explicitVersionPropertyUpdateMap );
        }

        logger.debug( "Applying overrides to concrete dependencies for: {}", projectGA );
        // Apply overrides to project direct dependencies
        applyOverrides( project, project.getResolvedDependencies( session ), explicitOverrides, originalOverrides );
        applyExplicitOverrides( project, project.getResolvedDependencies( session ), explicitOverrides, commonState,
                                explicitVersionPropertyUpdateMap );

        final Map<Profile, Map<ArtifactRef, Dependency>> pd = project.getResolvedProfileDependencies( session );
        final Map<Profile, Map<ArtifactRef, Dependency>> pmd = project.getResolvedProfileManagedDependencies( session );

        for ( Profile p : pd.keySet() )
        {
            applyOverrides( project, pd.get( p ), explicitOverrides, originalOverrides );
            applyExplicitOverrides( project, pd.get( p ), explicitOverrides, commonState,
                                    explicitVersionPropertyUpdateMap );
        }
        for ( Profile p : pmd.keySet() )
        {
            applyOverrides( project, pmd.get( p ), explicitOverrides, originalOverrides );
            applyExplicitOverrides( project, pmd.get( p ), explicitOverrides, commonState,
                                    explicitVersionPropertyUpdateMap );
        }
    }

    /**
     * Apply a set of version overrides to a list of dependencies. Return a set of the overrides which were not applied.
     *
     * @param project The current Project
     * @param dependencies The list of dependencies
     * @param explicitOverrides Any explicitOverrides to track for ignoring
     * @param overrides The map of dependency version overrides
     * @return The map of overrides that were not matched in the dependencies
     * @throws ManipulationException if an error occurs
     */
    private Map<ArtifactRef, String> applyOverrides( final Project project, final Map<ArtifactRef, Dependency> dependencies,
                                                     final WildcardMap<String> explicitOverrides, final Map<ArtifactRef, String> overrides )
                    throws ManipulationException
    {
        // Duplicate the override map so unused overrides can be easily recorded
        final Map<ArtifactRef, String> unmatchedVersionOverrides = new LinkedHashMap<>( overrides );

        if ( dependencies == null || dependencies.size() == 0 )
        {
            return unmatchedVersionOverrides;
        }

        final CommonState commonState = session.getState( CommonState.class );
        final boolean strict = commonState.isStrict();

        // Apply matching overrides to dependencies
        for ( final ArtifactRef dependency : dependencies.keySet() )
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
                    final String oldVersion = dependencies.get( dependency ).getVersion();
                    final String overrideVersion = entry.getValue();
                    final String resolvedValue = dependency.getVersionString();

                    if ( isEmpty( overrideVersion ) )
                    {
                        logger.warn( "Unable to align with an empty override version for {}; ignoring", groupIdArtifactId );
                    }
                    else if ( isEmpty( oldVersion ) )
                    {
                        logger.debug( "Dependency is a managed version for {}; ignoring", groupIdArtifactId );
                    }
                    else if (oldVersion.equals( Version.PROJECT_VERSION ) || ( oldVersion.contains( "$" ) && project.getVersion().equals( resolvedValue ) ) )
                    {
                        logger.debug( "Dependency {} with original version {} and project version {} for {} references ${project.version} so skipping.",
                                     dependency, oldVersion, project.getVersion(), project.getPom() );
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
                        logger.debug ("Original fully resolved version {} for {} does not match override version {} -> {} so ignoring",
                                      resolvedValue, dependency, entry.getKey(), overrideVersion);
                        if ( commonState.isFailOnStrictViolation() )
                        {
                            throw new ManipulationException(
                                            "For {} replacing original property version {} (fully resolved: {} ) with new version {} for {} violates the strict version-alignment rule!",
                                            depPr.toString(), dependencies.get( dependency ).getVersion(), resolvedValue, entry.getKey().getVersionString(), entry.getKey().asProjectRef().toString());
                        }
                        else
                        {
                            logger.warn( "Replacing original property version {} with new version {} for {} violates the strict version-alignment rule!",
                                         resolvedValue, overrideVersion, dependencies.get( dependency ).getVersion() );
                        }
                    }
                    else
                    {
                        if ( ! PropertiesUtils.cacheProperty( project, commonState, versionPropertyUpdateMap, oldVersion, overrideVersion, entry.getKey(), false ))
                        {
                            if ( strict && ! PropertiesUtils.checkStrictValue( session, resolvedValue, overrideVersion) )
                            {
                                if ( commonState.isFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                     "Replacing original version {} in dependency {} with new version {} violates the strict version-alignment rule!",
                                                     oldVersion, groupIdArtifactId, overrideVersion );
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

                                // This block handles a version that is a partial property with a value.
                                if ( oldVersion.contains( "${" ) )
                                {
                                    String suffix = PropertiesUtils.getSuffix( session );
                                    String replaceVersion;

                                    // Handles ${...}...-rebuild-n -> ${...}...-rebuild-n+1
                                    if ( commonState.isStrictIgnoreSuffix() && oldVersion.contains( suffix ) )
                                    {
                                        replaceVersion = StringUtils.substringBefore( oldVersion, suffix );
                                        replaceVersion += suffix + StringUtils.substringAfter( overrideVersion, suffix );
                                    }
                                    else
                                    {
                                        // It is feasible that even though ${foo}.x-suffix may look appropriate, foo may have
                                        // been updated by another dependency to foo-suffix so that we end up with
                                        // foo-suffix.x-suffix. Therefore just replace with overrideVersion
                                        replaceVersion = overrideVersion;
                                    }
                                    logger.debug ( "Resolved value is {} and replacement version is {} ", resolvedValue, replaceVersion );

                                    // In this case the previous value couldn't be cached even though it contained a property
                                    // as it was either multiple properties or a property combined with a hardcoded value. Therefore
                                    // just append the suffix.
                                    dependencies.get( dependency ).setVersion( replaceVersion );
                                }
                                else
                                {
                                    dependencies.get( dependency ).setVersion( overrideVersion );
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
    {
        final Map<ArtifactRef, String> reducedVersionOverrides = new LinkedHashMap<>( versionOverrides );
        for ( final Project project : session.getProjects() )
        {
            reducedVersionOverrides.remove( new SimpleArtifactRef( project.getGroupId(), project.getArtifactId(), project.getVersion(), "pom", null ) );
        }
        return reducedVersionOverrides;
    }

    private void validateDependenciesUpdatedProperty( CommonState cState, Project p, Map<ArtifactRef, Dependency> dependencies )
                    throws ManipulationException
    {
        for ( ArtifactRef d : dependencies.keySet() )
        {
            String versionProperty = dependencies.get( d ).getVersion();
            if ( versionProperty.startsWith( "${" ) )
            {
                PropertiesUtils.verifyPropertyMapping( cState, p, versionPropertyUpdateMap, d,
                                                       PropertiesUtils.extractPropertyName( versionProperty ) );
            }
        }
    }
}
