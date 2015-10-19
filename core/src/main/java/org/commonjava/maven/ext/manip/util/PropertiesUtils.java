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
package org.commonjava.maven.ext.manip.util;

import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.impl.DependencyManipulator;
import org.commonjava.maven.ext.manip.impl.Version;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.DependencyState;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
        final Map<String, String> matchedProperties = new HashMap<String, String>();
        final int prefixLength = prefix.length();

        for ( final String propertyName : properties.stringPropertyNames() )
        {
            if ( propertyName.startsWith( prefix ) )
            {
                final String trimmedPropertyName = propertyName.substring( prefixLength );
                String value = properties.getProperty( propertyName );
                if ( value.equals( "true" ) )
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
     * @param newValue a value to look for.   @return true if changes were made.
     * @throws ManipulationException
     */
    public static boolean updateProperties( ManipulationSession session, Set<Project> projects, boolean ignoreStrict,
                                            String key, String newValue )
                    throws ManipulationException
    {
        final DependencyState state = session.getState( DependencyState.class );
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
                    if ( !updateProperties( session, projects, ignoreStrict, oldValue.substring( 2, oldValue.indexOf( '}' ) ),
                                            newValue ) )
                    {
                        logger.error( "Recursive property not found for {} with {} ", oldValue, newValue );
                        return false;
                    }
                }
                else
                {
                    if ( state.getStrict() && !ignoreStrict )
                    {
                        if ( ! checkStrictValue( session, oldValue, newValue ) )
                        {
                            if ( state.getFailOnStrictViolation() )
                            {
                                throw new ManipulationException(
                                                "Replacing original property version {} with new version {} for {} violates the strict version-alignment rule!",
                                                 oldValue, newValue, key );
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

                    p.getModel().getProperties().setProperty( key, newValue );
                }
            }
        }
        return found;
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
        // New value might be e.g. 3.1-rebuild-1 or 3.1.0.rebuild-1 (i.e. it *might* be OSGi compliant).
        final VersioningState state = session.getState( VersioningState.class );
        final Version v = new Version( oldValue );

        String newVersion = newValue;
        String suffix = null;
        String osgiVersion = v.getOSGiVersionString();

        if ( state.getIncrementalSerialSuffix() != null && state.getIncrementalSerialSuffix().length() > 0)
        {
            suffix = state.getIncrementalSerialSuffix();
        }
        else if ( state.getSuffix() != null && state.getSuffix().length() > 0)
        {
            suffix = state.getSuffix().substring( 0, state.getSuffix().indexOf( '-' ) );
        }

        if ( suffix != null)
        {
            v.appendQualifierSuffix( suffix );
            osgiVersion = v.getOSGiVersionString();
            osgiVersion = osgiVersion.substring( 0, osgiVersion.indexOf( suffix ) - 1 );

            if ( newValue.contains( suffix ) )
            {
                newVersion = newValue.substring( 0, newValue.indexOf( suffix ) - 1 );
            }
        }

        logger.debug ( "Comparing original version {} and OSGi variant {} with new version {} and suffix removed {} " ,
                                             oldValue, osgiVersion, newValue, newVersion);

        // We compare both an OSGi'ied oldVersion and the non-OSGi version against the possible new version (which has
        // had its suffix stripped) in order to check whether its a valid change.
        boolean result = false;
        if ( oldValue != null && oldValue.equals( newVersion ) || osgiVersion.equals( newVersion ))
        {
            result = true;
        }
        return result;
    }
}
