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
package org.commonjava.maven.ext.manip.util;

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.impl.Version;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Commonly used manipulations / extractions from project / user (CLI) properties.
 */
public final class PropertiesUtils
{
    private final static Logger logger = LoggerFactory.getLogger( PropertiesUtils.class );

    private PropertiesUtils()
    {
    }

    /**
     * Filter Properties by accepting only properties with names that start with prefix. Trims the prefix
     * from the property names when inserting them into the returned Map.
     * @param properties the properties to filter.
     * @param prefix The String that must be at the start of the property names
     * @return map of properties with matching prepend and their values
     */
    public static Map<String, String> getPropertiesByPrefix( final Properties properties, final String prefix )
    {
        final Map<String, String> matchedProperties = new HashMap<>();
        final int prefixLength = prefix.length();

        for ( final String propertyName : properties.stringPropertyNames() )
        {
            if ( propertyName.startsWith( prefix ) )
            {
                final String trimmedPropertyName = propertyName.substring( prefixLength );
                String value = properties.getProperty( propertyName );
                if ( value != null && value.equals( "true" ) )
                {
                    logger.warn( "Work around Brew/Maven bug - removing erroneous 'true' value for {}.",
                                 trimmedPropertyName );
                    value = "";
                }
                matchedProperties.put( trimmedPropertyName, value );
            }
        }

        return matchedProperties;
    }

    /**
     * Recursively update properties.
     *
     * @param session the DependencyState
     * @param projects the current set of projects we are scanning.
     * @param ignoreStrict whether to ignore strict alignment.
     * @param key a key to look for.
     * @param newValue a value to look for.
     * @return {@code PropertyUpdate} enumeration showing status of any changes.
     * @throws ManipulationException if an error occurs
     */
    public static PropertyUpdate updateProperties( ManipulationSession session, Set<Project> projects, boolean ignoreStrict,
                                                   String key, String newValue ) throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );
        PropertyUpdate found = PropertyUpdate.NOTFOUND;

        final String resolvedValue = resolveProperties( new ArrayList<>( projects ), "${" + key + '}' );
        logger.debug( "Fully resolvedValue is {} for {} ", resolvedValue, key );

        if ( resolvedValue.equals( newValue ) )
        {
            logger.warn( "Nothing to update as original key {} value matches new value {} ", key, newValue );
            return PropertyUpdate.IGNORE;
        }
        else if ( "project.version".equals( key ) )
        {
            logger.warn( "Not updating key {} with {} ", key, newValue );
            return PropertyUpdate.IGNORE;
        }

        for ( final Project p : projects )
        {
            if ( p.getModel().getProperties().containsKey( key ) )
            {
                final String oldValue = p.getModel().getProperties().getProperty( key );

                logger.info( "Updating property {} / {} with {} ", key, oldValue, newValue );

                found = PropertyUpdate.FOUND;

                // We'll only recursively resolve the property if its a single >${foo}<. If its one of
                // >${foo}value${foo}<
                // >${foo}${foo}<
                // >value${foo}<
                // >${foo}value<
                // it becomes hairy to verify strict compliance and to correctly split the old value and
                // update it with a portion of the new value.
                if ( oldValue != null && oldValue.startsWith( "${" ) && oldValue.endsWith( "}" ) &&
                                !( StringUtils.countMatches( oldValue, "${" ) > 1 ) )
                {
                    logger.debug( "Recursively resolving {} ", oldValue.substring( 2, oldValue.length() - 1 ) );

                    if ( updateProperties( session, projects, ignoreStrict,
                                            oldValue.substring( 2, oldValue.length() - 1 ), newValue ) == PropertyUpdate.NOTFOUND )
                    {
                        logger.error( "Recursive property not found for {} with {} ", oldValue, newValue );
                        return PropertyUpdate.NOTFOUND;
                    }
                }
                else
                {
                    if ( state.getStrict() && !ignoreStrict )
                    {
                        if ( !checkStrictValue( session, resolvedValue, newValue ) )
                        {
                            if ( state.getFailOnStrictViolation() )
                            {
                                throw new ManipulationException(
                                                "Replacing original property version {} (fully resolved: {} ) with new version {} for {} violates the strict version-alignment rule!",
                                                oldValue, resolvedValue, newValue, key );
                            }
                            else
                            {
                                logger.warn( "Replacing original property version {} with new version {} for {} violates the strict version-alignment rule!",
                                             oldValue, newValue, key );
                                // Ignore the dependency override. As found has been set to true it won't inject
                                // a new property either.
                                continue;
                            }
                        }
                    }

                    // TODO: Does not handle explicit overrides.
                    if ( oldValue != null && oldValue.contains( "${" ) &&
                                    !( oldValue.startsWith( "${" ) && oldValue.endsWith( "}" ) ) || (
                                    StringUtils.countMatches( oldValue, "${" ) > 1 ) )
                    {
                        // This block handles
                        // >${foo}value${foo}<
                        // >${foo}${foo}<
                        // >value${foo}<
                        // >${foo}value<
                        // We don't attempt to recursively resolve those as tracking the split of the variables, combined
                        // with the update and strict version checking becomes overly fragile.

                        if ( ignoreStrict )
                        {
                            throw new ManipulationException(
                                            "NYI : handling for versions with explicit overrides (" + oldValue + ") with multiple embedded properties is NYI. " );
                        }
                        newValue = oldValue + StringUtils.removeStart( newValue, resolvedValue );
                        logger.info( "Ignoring new value due to embedded property {} and appending {} ", oldValue,
                                     newValue );
                    }

                    p.getModel().getProperties().setProperty( key, newValue );
                }
            }
        }
        return found;
    }

    /**
     * Retrieve any configured rebuild suffix.
     * @param session Current ManipulationSession
     * @return string suffix.
     */
    public static String getSuffix (ManipulationSession session)
    {
        final VersioningState versioningState = session.getState( VersioningState.class );
        String suffix = null;

        if ( versioningState.getIncrementalSerialSuffix() != null && !versioningState.getIncrementalSerialSuffix().isEmpty() )
        {
            suffix = versioningState.getIncrementalSerialSuffix();
        }
        else if ( versioningState.getSuffix() != null && !versioningState.getSuffix().isEmpty() )
        {
            suffix = versioningState.getSuffix().substring( 0, versioningState.getSuffix().indexOf( '-' ) );
        }

        return suffix;
    }

    /**
     * Check the version change is valid in strict mode.
     *
     * @param session the manipulation session
     * @param oldValue the original version
     * @param newValue the new version
     * @return true if the version can be changed to the new version
     */
    public static boolean checkStrictValue( ManipulationSession session, String oldValue, String newValue )
    {
        if ( oldValue == null || newValue == null )
        {
            return false;
        }
        else if ( oldValue.equals( newValue ) )
        {
            // The old version and new version matches. So technically it can be changed (even if its a bit pointless).
            return true;
        }

        final DependencyState dState = session.getState( DependencyState.class );
        final boolean ignoreSuffix = dState.getStrictIgnoreSuffix();

        // New value might be e.g. 3.1-rebuild-1 or 3.1.0.rebuild-1 (i.e. it *might* be OSGi compliant).
        String newVersion = newValue;
        String suffix = getSuffix( session );

        Version v = new Version( oldValue );
        String osgiVersion = v.getOSGiVersionString();

        // If we have been configured to ignore the suffix (e.g. rebuild-n) then, assuming that
        // the oldValue actually contains the suffix process it.
        if ( ignoreSuffix && oldValue.contains( suffix ))
        {
            HashSet<String> s = new HashSet<>();
            s.add( oldValue );
            s.add( newValue );

            String x = String.valueOf( Version.findHighestMatchingBuildNumber( v, s ) );

            // If the new value has the higher matching build number strip the old suffix to allow for strict
            // matching.
            if ( newValue.endsWith( x ) )
            {
                String oldValueCache = oldValue;
                oldValue = oldValue.substring( 0, oldValue.indexOf( suffix ) - 1 );
                v = new Version( oldValue );
                osgiVersion = v.getOSGiVersionString();
                logger.debug( "Updating version to {} and for oldValue {} with newValue {} ", v, oldValueCache, newValue );

            }
            else
            {
                logger.warn( "strictIgnoreSuffix set but unable to align from {} to {}", oldValue, newValue );
            }
        }

        // We only need to dummy up and add a suffix if there is no qualifier. This allows us
        // to work out the OSGi version.
        if ( suffix != null && !v.hasQualifier() )
        {
            v.appendQualifierSuffix( suffix );
            osgiVersion = v.getOSGiVersionString();
            osgiVersion = osgiVersion.substring( 0, osgiVersion.indexOf( suffix ) - 1 );
        }
        if ( suffix != null && newValue.contains( suffix ) )
        {
            newVersion = newValue.substring( 0, newValue.indexOf( suffix ) - 1 );
        }
        logger.debug( "Comparing original version {} and OSGi variant {} with new version {} and suffix removed {} ",
                      oldValue, osgiVersion, newValue, newVersion );

        // We compare both an OSGi'ied oldVersion and the non-OSGi version against the possible new version (which has
        // had its suffix stripped) in order to check whether its a valid change.
        boolean result = false;
        if ( oldValue.equals( newVersion ) || osgiVersion.equals( newVersion ) )
        {
            result = true;
        }
        return result;
    }

    /**
     * This will check if the old version (e.g. in a plugin or dependency) is a property and if so
     * store the mapping in a map.
     *
     * @param versionPropertyUpdateMap the map to store any updates in
     * @param oldVersion original property value
     * @param newVersion new property value
     * @param originalType that this property is used in (i.e. a plugin or a dependency)
     * @param force Whether to check for an existing property or force the insertion
     * @return true if a property was found and cached.
     * @throws ManipulationException if an error occurs.
     */
    public static boolean cacheProperty( Map<String, String> versionPropertyUpdateMap, String oldVersion,
                                         String newVersion, Object originalType, boolean force )
                    throws ManipulationException
    {
        boolean result = false;
        if ( oldVersion != null && oldVersion.contains( "${" ) )
        {
            final int endIndex = oldVersion.indexOf( '}' );
            final String oldProperty = oldVersion.substring( 2, endIndex );

            // We don't attempt to cache any value that contains more than one property or contains a property
            // combined with a hardcoded value.
            if ( oldVersion.contains( "${" ) && !( oldVersion.startsWith( "${" ) && oldVersion.endsWith( "}" ) ) || (
                            StringUtils.countMatches( oldVersion, "${" ) > 1 ) )
            {
                logger.debug( "For {} ; original version contains hardcoded value or multiple embedded properties. Not caching value ( {} -> {} )",
                              originalType, oldVersion, newVersion );
            }
            else if ( "project.version".equals( oldProperty ) )
            {
                logger.debug( "For {} ; original version was a property mapping. Not caching value as property is built-in ( {} -> {} )",
                              originalType, oldProperty, newVersion );
            }
            else
            {
                logger.debug( "For {} ; original version was a property mapping; caching new value for update {} -> {}",
                              originalType, oldProperty, newVersion );

                final String oldVersionProp = oldVersion.substring( 2, oldVersion.length() - 1 );

                // We check if we are replacing a property and there is already a mapping. While we don't allow
                // a property to be updated to two different versions, if a dependencyExclusion (i.e. a force override)
                // has been specified this will bypass the check.
                String existingPropertyMapping = versionPropertyUpdateMap.get( oldVersionProp );

                if ( existingPropertyMapping != null && !existingPropertyMapping.equals( newVersion ) )
                {
                    if ( force )
                    {
                        logger.debug( "Override property replacement of {} with force version override {}",
                                      existingPropertyMapping, newVersion );
                    }
                    else
                    {
                        logger.error( "Replacing property '{}' with a new version but the existing version does not match. Old value is {} and new is {}",
                                      oldVersionProp, existingPropertyMapping, newVersion );
                        throw new ManipulationException(
                                        "Property replacement clash - updating property '{}' to both {} and {} ",
                                        oldVersionProp, existingPropertyMapping, newVersion );
                    }
                }

                versionPropertyUpdateMap.put( oldVersionProp, newVersion );

                result = true;
            }
        }
        return result;
    }

    /**
     * This recursively checks the supplied version and recursively resolves it if its a property.
     *
     * @param projects set of projects
     * @param value value to check
     * @return the version string
     * @throws ManipulationException if an error occurs
     */
    public static String resolveProperties( List<Project> projects, String value ) throws ManipulationException
    {
        final Properties amalgamated = new Properties();

        // Reverse order so execution root ends up overwriting everything.
        for ( int i = projects.size() - 1; i >= 0; i-- )
        {
            amalgamated.putAll( projects.get( i ).getModel().getProperties() );
        }
        PropertyInterpolator pi = new PropertyInterpolator( amalgamated, projects.get( 0 ) );
        return pi.interp( value );
    }

    /**
     * Used to determine whether any property updates were successful of not. In the case of detecting that no properties are
     * needed IGNORE is returned. Effectively this is a slightly more explicit tri-state.
     */
    public enum PropertyUpdate
    {
        FOUND,
        NOTFOUND,
        IGNORE
    }
}
