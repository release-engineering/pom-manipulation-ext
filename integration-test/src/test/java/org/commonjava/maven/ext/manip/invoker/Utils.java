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
package org.commonjava.maven.ext.manip.invoker;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author vdedik@redhat.com
 */
public class Utils
{

    /**
     * Loads *.properties file.
     *
     * @param filePath - File path of the *.properties file
     * @return Loaded properties
     */
    public static Properties loadProps( String filePath )
    {
        File propsFile = new File( filePath );
        Properties props = new Properties();
        if ( propsFile.isFile() )
        {
            try
            {
                FileInputStream fis = new FileInputStream( propsFile );
                props.load( fis );
            }
            catch ( Exception e )
            {
                // ignore
            }
        }

        return props;
    }

    public static Map<String, String> propsToMap( Properties props )
    {
        Map<String, String> map = new HashMap<String, String>();
        for ( Object p : props.keySet() )
        {
            map.put( (String) p, props.getProperty( (String) p ) );
        }

        return map;
    }
}
