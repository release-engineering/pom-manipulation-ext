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

import org.apache.maven.model.Exclusion;
import org.apache.maven.model.InputLocationTracker;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.DependencyPluginWrapper;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.util.WildcardMap;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.CommonState;
import org.commonjava.maven.ext.core.util.PropertiesUtils;
import org.commonjava.maven.ext.core.util.PropertyMapper;
import org.commonjava.maven.ext.io.ModelIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

/**
 * Shared base class for Plugin and Dependency manipulator - enables sharing of some common code.
 */
public class CommonManipulator
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Used to store mappings of old property to new version for explicit overrides.
     */
    protected final Map<Project,Map<String, PropertyMapper>> explicitVersionPropertyUpdateMap = new LinkedHashMap<>();

    protected ModelIO effectiveModelBuilder;

    protected ManipulationSession session;

    /**
     * Remove module overrides which do not apply to the current module. Searches the full list of version overrides
     * for any keys which contain the '@' symbol.  Removes these from the version overrides list, and add them back
     * without the '@' symbol only if they apply to the current module.
     *
     * @param projectGA the current project group : artifact
     * @param moduleOverrides are individual overrides e.g. group:artifact@groupId:artifactId :: value
     * @param originalOverrides The full list of version overrides, both global and module specific
     * @param explicitOverrides a custom map to handle wildcard overrides
     * @param extraBOMOverrides a nested map of additional overrides, keyed on a String
     * @return The map of global and module specific overrides which apply to the given module
     * @throws ManipulationException if an error occurs
     */
    protected Map<ArtifactRef, String> applyModuleVersionOverrides( final String projectGA,
                                                                           final Map<String, String> moduleOverrides,
                                                                           final Map<ArtifactRef, String> originalOverrides,
                                                                           final WildcardMap<String> explicitOverrides,
                                                                           final Map<String, Map<ProjectRef, String>> extraBOMOverrides )
                    throws ManipulationException
    {
        final Map<ArtifactRef, String> remainingOverrides = new LinkedHashMap<>( originalOverrides );

        if (logger.isDebugEnabled())
        {
            logger.debug( "Calculating module-specific version overrides. Starting with:{}  {}", System.lineSeparator(),
                    join( remainingOverrides.entrySet(), System.lineSeparator() + "  " ) );
        }

        // These modes correspond to two different kinds of passes over the available override properties:
        // 1. Module-specific: Don't process wildcard overrides here, allow module-specific settings to take precedence.
        // 2. Wildcards: Add these IF there is no corresponding module-specific override.
        final boolean[] wildcardMode = { false, true };
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

                String artifactGA;
                boolean replace = false;
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
                        throw new ManipulationException( "Invalid format for exclusion key {}", currentKey );
                    }
                    artifactGA = artifactAndModule[0];
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
                            replace = true;
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

                    artifactGA = currentKey.substring( 0, currentKey.length() - 2 );
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
                        replace = true;
                    }
                    else
                    {
                        // If we have a wildcard artifact we want to replace any prior explicit overrides
                        // with this one i.e. this takes precedence.
                        removeGA( remainingOverrides, SimpleProjectRef.parse( artifactGA ) );
                        logger.debug( "Removing artifactGA {} from overrides", artifactGA );
                    }
                }

                if ( replace )
                {
                    ProjectRef projectRef = SimpleProjectRef.parse( artifactGA );
                    String newArtifactValue;
                    // Expand values that reference an extra BOM
                    Map<ProjectRef, String> extraBOM = extraBOMOverrides.get( currentValue );
                    if ( extraBOM == null )
                    {
                        newArtifactValue = currentValue;
                    }
                    else
                    {
                        newArtifactValue = extraBOM.get( projectRef );
                        if ( newArtifactValue == null )
                        {
                            throw new ManipulationException( "Extra BOM {} does not define a version for artifact {} targeted by {}",
                                                             currentValue, artifactGA, currentKey );
                        }
                        logger.debug( "Dereferenced value {} for {} from extra BOM {}", newArtifactValue, artifactGA,
                                      currentValue );
                    }
                    explicitOverrides.put( projectRef, newArtifactValue );
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

    /**
     * Apply explicit overrides to a set of dependencies from a project. The explicit overrides come from
     * dependencyExclusion. However they have to be separated out from standard overrides so we can easily
     * ignore any property references (and overwrite them).
     *
     * @param project the current Project
     * @param dependencies dependencies to check
     * @param explicitOverrides a custom map to handle wildcard overrides
     * @param state the CommonState, to retrieve Common Properties
     * @param versionPropertyUpdateMap properties to update
     * @throws ManipulationException if an error occurs
     */
    protected void applyExplicitOverrides( final Project project,
                                           final Map<? extends ProjectVersionRef, ? extends InputLocationTracker> dependencies,
                                           final WildcardMap<String> explicitOverrides, final CommonState state,
                                           final Map<Project, Map<String, PropertyMapper>> versionPropertyUpdateMap )
                    throws ManipulationException
    {
        // Apply matching overrides to dependencies
        for ( final ProjectVersionRef projectVersionRef : dependencies.keySet() )
        {
            final ProjectRef groupIdArtifactId = new SimpleProjectRef( projectVersionRef.getGroupId(), projectVersionRef.getArtifactId() );

            if ( explicitOverrides.containsKey( groupIdArtifactId ) )
            {
                final String overrideVersion = explicitOverrides.get( groupIdArtifactId );
                final DependencyPluginWrapper wrapper = new DependencyPluginWrapper( dependencies.get( projectVersionRef ) );
                final String oldVersion = wrapper.getVersion();

                if ( isEmpty( overrideVersion ) || isEmpty( oldVersion ) )
                {
                    if ( isEmpty( oldVersion ) )
                    {
                        logger.debug( "Unable to force align as no existing version field to update for " + groupIdArtifactId + "; ignoring" );
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
                        if ( target.startsWith( "+" ) )
                        {
                            logger.info( "Adding dependency exclusion {} to dependency {} ", target.substring( 1 ),
                                         projectVersionRef );
                            Exclusion e = new Exclusion();
                            e.setGroupId( target.substring( 1 ).split( ":" )[0] );
                            e.setArtifactId( target.split( ":" )[1] );
                            wrapper.addExclusion( e );
                        }
                        else
                        {
                            logger.info( "Explicit overrides : force aligning {} to {}.", groupIdArtifactId, target );

                            if ( !PropertiesUtils.cacheProperty( project, state, versionPropertyUpdateMap, oldVersion,
                                                                 target, projectVersionRef, true ) )
                            {
                                if ( oldVersion.contains( "${" ) )
                                {
                                    // This might happen if the property was inherited from an external parent.
                                    logger.warn( "Overriding version with {} when old version contained a property {} "
                                                                 + "that was not found in the current project",
                                                 target, oldVersion );
                                }
                                // Not checking strict version alignment here as explicit overrides take priority.
                                wrapper.setVersion( target );
                            }
                        }
                    }
                }
            }
        }
    }


    protected void explicitOverridePropertyUpdates( ManipulationSession session ) throws ManipulationException
    {
        // Moved this to debug as otherwise it deluges the logging.
        logger.debug ("Iterating for explicit overrides...");
        for ( Project project : explicitVersionPropertyUpdateMap.keySet() )
        {
            logger.debug( "Checking property override within project {} ", project );
            for ( final Map.Entry<String, PropertyMapper> entry : explicitVersionPropertyUpdateMap.get( project ).entrySet() )
            {
                PropertiesUtils.PropertyUpdate found =
                                PropertiesUtils.updateProperties( session, project, true,
                                                                  entry.getKey(), entry.getValue().getNewVersion() );

                if ( found == PropertiesUtils.PropertyUpdate.NOTFOUND )
                {
                    // Problem in this scenario is that we know we have a property update map but we have not found a
                    // property to update. Its possible this property has been inherited from a parent. Override in the
                    // top pom for safety.
                    logger.info( "Unable to find a property for {} to update for explicit overrides", entry.getKey() );
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
    }
}
