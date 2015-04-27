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

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.commonjava.maven.ext.manip.util.PropertiesUtils.getPropertiesByPrefix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.VersionlessArtifactRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.DependencyState.VersionPropertyFormat;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.State;
import org.commonjava.maven.ext.manip.util.WildcardMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Manipulator} implementation that can alter dependency (and dependency management) sections in a project's pom file.
 * Configuration is stored in a {@link DependencyState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "project-dependency-manipulator" )
public class DependencyManipulator
    implements Manipulator
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected ModelIO effectiveModelBuilder;


    /**
     * Initialize the {@link DependencyState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link org.commonjava.maven.ext.manip.impl.Manipulator#scan(List, ManipulationSession)} and the apply* methods.
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

        final Set<Project> result = new HashSet<Project>();
        final boolean strict = state.getStrict();
        final Map<String, String> versionPropertyUpdateMap = state.getVersionPropertyUpdateMap();

        final Map<ProjectRef, String> overrides = loadRemoteBOM( state, session );

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
        if (result.size() > 0)
        {
            for (final String key : versionPropertyUpdateMap.keySet())
            {
                boolean found = false;

                for (final Project p : result)
                {
                    if ( p.getModel().getProperties().containsKey (key) )
                    {
                        logger.info( "Updating property {} with {} ", key, versionPropertyUpdateMap.get( key ) );
                        final String oldValue = p.getModel().getProperties().getProperty( key );
                        final String overrideVersion = versionPropertyUpdateMap.get( key );

                        found = true;

                        if ( strict )
                        {
                            if ( oldValue != null && !overrideVersion.startsWith( oldValue ) )
                            {
                                if ( state.getFailOnStrictViolation() )
                                {
                                    throw new ManipulationException(
                                                                     "Replacement: {} of original version: {} in property: {} violates the strict version-alignment rule!",
                                                                     overrideVersion, oldValue, key );
                                }
                                else
                                {
                                    logger.warn( "Replacement: {} of original version: {} in property: {} violates the strict version-alignment rule!",
                                                 overrideVersion, oldValue, key );
                                    // Ignore the dependency override. As found has been set to true it won't inject
                                    // a new property either.
                                    continue;
                                }
                            }
                        }

                        p.getModel().getProperties().setProperty( key, versionPropertyUpdateMap.get( key ) );
                    }
                }

                if ( found == false )
                {
                    // Problem in this scenerio is that we know we have a property update map but we have not found a
                    // property to update. Its possible this property has been inherited from a parent. Override in the
                    // top pom for safety.
                    logger.info( "Unable to find a property for {} to update", key );
                    for (final Project p : result)
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

    private Map<ProjectRef, String> loadRemoteBOM( final State state, final ManipulationSession session )
        throws ManipulationException
    {
        final Map<ProjectRef, String> overrides = new LinkedHashMap<ProjectRef, String>();
        final List<ProjectVersionRef> gavs = ((DependencyState)state).getRemoteDepMgmt();

        if ( gavs == null || gavs.isEmpty() )
        {
            return overrides;
        }

        final ListIterator<ProjectVersionRef> iter = gavs.listIterator( gavs.size() );
        // Iterate in reverse order so that the first GAV in the list overwrites the last
        while ( iter.hasPrevious() )
        {
            final ProjectVersionRef ref = iter.previous();
            overrides.putAll( effectiveModelBuilder.getRemoteDependencyVersionOverrides( ref, session ) );
        }

        return overrides;
    }

    /**
     * Applies dependency overrides to the project.
     *
     * The overrides ProjectRef:version map has to be converted into Group|Artifact:Version map
     * for usage by exclusions.
     */
    private void apply( final ManipulationSession session, final Project project, final Model model,
                          final Map<ProjectRef, String> overrides )
        throws ManipulationException
    {
        // Map of Group : Map of artifactId [ may be wildcard ] : value
        final WildcardMap explicitOverrides = new WildcardMap();
        final DependencyState state = session.getState( DependencyState.class );
        final Map<String, String> versionPropertyUpdateMap = state.getVersionPropertyUpdateMap();

        final String projectGA = ga( project );

        // TODO: FIXME: Is it possible to avoid the secondary override map and just convert everything
        // to projectref's as required?

        // Convert into a Map of GA : version
        Map<String, String> moduleOverrides = new LinkedHashMap<String, String>();
        for (final ProjectRef var : overrides.keySet())
        {
            moduleOverrides.put( var.asProjectRef()
                                    .toString(), overrides.get( var ) );
        }

        logger.debug( "Adding in dependency-exclusion properties..." );
        moduleOverrides.putAll( getPropertiesByPrefix( session.getUserProperties(),
                                                         DependencyState.DEPENDENCY_EXCLUSION_PREFIX ) );

        moduleOverrides = removeReactorGAs( session, moduleOverrides );
        moduleOverrides = applyModuleVersionOverrides( projectGA, moduleOverrides, explicitOverrides );

        if ( project.isInheritanceRoot() )
        {
            // Handle the situation where the top level parent refers to a prior build that is in the BOM.
            if ( project.getParent() != null && moduleOverrides.containsKey( ga( project.getParent() ) ) )
            {
                logger.debug( " Modifying parent reference from {} to {}",
                              model.getParent().getVersion(), moduleOverrides.get( ga( project.getParent() ) ));
                model.getParent()
                     .setVersion( moduleOverrides.get( ga( project.getParent() ) ) );
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

                final Map<String, String> nonMatchingVersionOverrides =
                    applyOverrides( session, project, dependencies, moduleOverrides );

                final Map<String, String> matchedOverrides = new LinkedHashMap<String, String>(moduleOverrides);
                matchedOverrides.keySet().removeAll( nonMatchingVersionOverrides.keySet() );

                applyExplicitOverrides( versionPropertyUpdateMap, explicitOverrides, dependencies );

                // Add/override a property to the build for each override
                addVersionOverrideProperties( session, matchedOverrides, model.getProperties() );

                if ( session.getState( DependencyState.class ).getOverrideTransitive() )
                {
                    final List<Dependency> extraDeps = new ArrayList<Dependency>();

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

                        final String artifactVersion = moduleOverrides.get( var.asProjectRef()
                                                                               .toString() );
                        newDependency.setVersion( artifactVersion );

                        extraDeps.add (newDependency);
                        logger.debug( "New entry added to <DependencyManagement/> - {} : {} ", projectRef, artifactVersion );

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
            if ( session.getState( DependencyState.class ).getOverrideDependencies() &&
                            dependencyManagement != null )
            {
                logger.debug( "Applying overrides to managed dependencies for: {}\n{}", projectGA, moduleOverrides );
                applyOverrides( session, project, dependencyManagement.getDependencies(), moduleOverrides );
                applyExplicitOverrides( versionPropertyUpdateMap, explicitOverrides, dependencyManagement.getDependencies() );
            }
            else
            {
                logger.debug( "NOT applying overrides to managed dependencies for: {}\n{}", projectGA, moduleOverrides );
            }
        }

        if (session.getState( DependencyState.class ).getOverrideDependencies() )
        {
            logger.debug( "Applying overrides to concrete dependencies for: {}\n{}", projectGA, moduleOverrides );
            // Apply overrides to project direct dependencies
            final List<Dependency> projectDependencies = model.getDependencies();
            applyOverrides( session, project, projectDependencies, moduleOverrides );
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
    private void applyExplicitOverrides( final Map<String, String> versionPropertyUpdateMap, final WildcardMap explicitOverrides, final List<Dependency> dependencies ) throws ManipulationException
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
                    if (oldVersion == null || oldVersion.length() == 0 )
                    {
                        logger.warn( "Unable to force align as no existing version field to update for " + groupIdArtifactId + "; ignoring" );
                    }
                    else
                    {
                        logger.warn( "Unable to force align as override version is empty for " + groupIdArtifactId + "; ignoring" );
                    }
                }
                else
                {
                    logger.debug( "Force aligning {} to {}.", groupIdArtifactId, overrideVersion );

                    if (oldVersion.startsWith( "${" ) )
                    {
                        final int endIndex = oldVersion.indexOf( '}');
                        final String oldProperty = oldVersion.substring( 2, endIndex);

                        if (endIndex != oldVersion.length() - 1)
                        {
                            throw new ManipulationException
                            ("NYI : handling for versions (" + oldVersion + ") with multiple embedded properties is NYI. ");
                        }
                        logger.debug ("Original version was a property mapping; caching new fixed value for update {} -> {}",
                                      oldProperty, overrideVersion);

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
     * @param session
     *
     * @param dependencies The list of dependencies
     * @param overrides The map of dependency version overrides
     * @return The map of overrides that were not matched in the dependencies
     * @throws ManipulationException
     */
    private Map<String, String> applyOverrides( final ManipulationSession session, final Project project,
                                                final List<Dependency> dependencies, final Map<String, String> overrides )
        throws ManipulationException
    {
        // Duplicate the override map so unused overrides can be easily recorded
        final Map<String, String> unmatchedVersionOverrides = new LinkedHashMap<String, String>();
        unmatchedVersionOverrides.putAll( overrides );

        if ( dependencies == null )
        {
            return unmatchedVersionOverrides;
        }

        final DependencyState state = session.getState( DependencyState.class );
        final Map<String, String> versionPropertyUpdateMap = state.getVersionPropertyUpdateMap();
        final boolean strict = state.getStrict();

        // Apply matching overrides to dependencies
        for ( final Dependency dependency : dependencies )
        {
            final String groupIdArtifactId = ga( dependency.getGroupId(), dependency.getArtifactId() );
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
                    // Handle the situation where we are updating a dependency that has an existing property - in this
                    // case we want to update the property instead.
                    // TODO: Handle the scenario where the version might be ${....}${....}
                    if (oldVersion.startsWith( "${" ) )
                    {
                        final int endIndex = oldVersion.indexOf( '}');
                        final String oldProperty = oldVersion.substring( 2, endIndex);

                        if (endIndex != oldVersion.length() - 1)
                        {
                            throw new ManipulationException
                                ("NYI : handling for versions (" + oldVersion + ") with multiple embedded properties is NYI. ");
                        }
                        logger.debug ("Original version was a property mapping; caching new value for update {} -> {}",
                                     oldProperty, overrideVersion);

                        final String oldVersionProp = oldVersion.substring( 2, oldVersion.length() - 1 );

                        versionPropertyUpdateMap.put( oldVersionProp, overrideVersion );
                    }
                    else
                    {
                        if ( strict && !overrideVersion.startsWith( oldVersion ) )
                        {
                            if ( state.getFailOnStrictViolation() )
                            {
                                throw new ManipulationException(
                                                                "Replacement: {} of original version: {} in dependency: {} violates the strict version-alignment rule!",
                                                                overrideVersion, oldVersion, groupIdArtifactId );
                            }
                            else
                            {
                                logger.warn( "Replacement: {} of original version: {} in dependency: {} violates the strict version-alignment rule!",
                                             overrideVersion, oldVersion, groupIdArtifactId );
                            }
                        }
                        else
                        {
                            logger.debug( "Altered dependency {} {} -> {}", groupIdArtifactId, oldVersion, overrideVersion );
                            dependency.setVersion( overrideVersion );
                        }
                    }
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
        final Map<String, String> reducedVersionOverrides = new LinkedHashMap<String, String>( versionOverrides );
        for ( final Project project : session.getProjects())
        {
            final String reactorGA = ga( project.getModel() );
            reducedVersionOverrides.remove( reactorGA );
        }
        return reducedVersionOverrides;
    }

    /**
     * Remove module overrides which do not apply to the current module. Searches the full list of version overrides
     * for any keys which contain the '@' symbol.  Removes these from the version overrides list, and add them back
     * without the '@' symbol only if they apply to the current module.
     *
     * @param projectGA
     * @param originalOverrides The full list of version overrides, both global and module specific
     * @param explicitOverrides
     *
     * @return The map of global and module specific overrides which apply to the given module
     * @throws ManipulationException
     */
    private Map<String, String> applyModuleVersionOverrides( final String projectGA,
                                                             final Map<String, String> originalOverrides, final WildcardMap explicitOverrides )
        throws ManipulationException
    {
        final Map<String, String> remainingOverrides = new LinkedHashMap<String, String>( originalOverrides );

        logger.debug( "Calculating module-specific version overrides. Starting with:\n  {}",
                      join(remainingOverrides.entrySet(), "\n  ") );

        final Map<String, String> moduleVersionOverrides = new LinkedHashMap<String, String>();
        final Set<String> processedKeys = new HashSet<String>();

        // These modes correspond to two different kinds of passes over the available override properties:
        // 1. Module-specific: Don't process wildcard overrides here, allow module-specific settings to take precedence.
        // 2. Wildcards: Add these IF there is no corresponding module-specific override.
        final boolean wildcardMode[] = { false, true };
        for ( int i = 0; i < wildcardMode.length; i++ )
        {
            for ( final String currentKey : new HashSet<String>(remainingOverrides.keySet()))
            {
                logger.debug( "Processing key for override: {}", currentKey );

                if ( !currentKey.contains( "@" ) )
                {
                    logger.debug( "Not an override. Skip." );
                    continue;
                }

                // add to list of processed keys to prevent it from being transferred over at the end, from the remainingKeys map.
                processedKeys.add( currentKey );

                final String currentValue = remainingOverrides.get( currentKey );
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
                            explicitOverrides.put( ProjectRef.parse(artifactGA), currentValue );
                            logger.debug( "Overriding module dependency for {} with {} : {}", moduleGA, artifactGA,
                                          currentValue );
                        }
                        else
                        {
                            //remove from remaining, since it's set to an empty value to disable override from the BOM
                            remainingOverrides.remove( artifactGA );
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

                    if ( explicitOverrides.containsKey( ProjectRef.parse(artifactGA) ) )
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
                        explicitOverrides.put(ProjectRef.parse(artifactGA), currentValue);
                    }
                    else
                    {
                        // If we have a wildcard artifact we want to replace any prior explicit overrides
                        // with this one i.e. this takes precedence.
                        if ( artifactGA.endsWith(":*"))
                        {
                            ProjectRef artifactGAPr = ProjectRef.parse(artifactGA);
                            Iterator<String> it = remainingOverrides.keySet().iterator();
                            while (it.hasNext())
                            {
                                ProjectRef pr = ProjectRef.parse(it.next());
                                if ( artifactGAPr.getGroupId().equals(pr.getGroupId()))
                                {
                                    logger.debug ( "Removing artifactGA " + pr + " from overrides");
                                    it.remove();
                                }
                            }
                        }
                        else
                        {
                            //remove from remaining, since it's set to an empty value to disable override from the BOM
                            remainingOverrides.remove(artifactGA);
                            logger.debug("Removing artifactGA " + artifactGA + " from overrides");
                        }
                        logger.debug( "Ignoring module dependency override for {} " + projectGA );
                    }
                }
            }
        }

        // now, go back and fill in any overrides coming from BOMs that weren't overridden or otherwise processed (eg. irrelevant module-specific overrides)
        for ( final Map.Entry<String, String> entry : remainingOverrides.entrySet() )
        {
            final String key = entry.getKey();
            if ( !processedKeys.contains( key ) && !moduleVersionOverrides.containsKey( key ) )
            {
                final String value = entry.getValue();
                logger.debug( "back-filling with override from original map: '{}' = '{}'", key, value );

                moduleVersionOverrides.put( key, value );
            }
        }

        logger.debug( "Returning module-specific overrides:\n{}", join( moduleVersionOverrides.entrySet(), "\n  " ) );

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
                                                                        VersionPropertyFormat.NONE.toString() ).toUpperCase() ) )
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
