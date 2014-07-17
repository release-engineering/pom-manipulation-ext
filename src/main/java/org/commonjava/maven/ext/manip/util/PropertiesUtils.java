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
     * @param properties
     *
     * @param prepend The String that must be at the start of the property names
     * @return Map<String, String> map of properties with matching prepend and their values
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
