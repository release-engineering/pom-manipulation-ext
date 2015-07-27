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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Commonly used manipulations / extractions from project / user (CLI) properties.
 */
public final class PropertiesUtils
{

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
        // init logger here to avoid loading it statically.
        final Logger logger = LoggerFactory.getLogger( PropertiesUtils.class );

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

}
