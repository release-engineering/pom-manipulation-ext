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

package org.commonjava.maven.ext.manip.io;

import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.YamlFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component( role = ConfigIO.class )
public class ConfigIO
{
    private final Logger logger = LoggerFactory.getLogger( ConfigIO.class );

    private final static String propertyFileString = "pme.properties";
    private final static String yamlFileString = "pme.yaml";

    public Properties parse ( final String workingDir) throws ManipulationException
    {
        return parse ( new File ( workingDir ) );
    }

    public Properties parse ( final File workingDir) throws ManipulationException
    {
        Properties result = new Properties( );
        File propertyFile = new File( workingDir, propertyFileString );
        File yamlFile = new File( workingDir, yamlFileString );

        if ( propertyFile.exists() && yamlFile.exists() )
        {
            throw new ManipulationException( "Cannot have both yaml and property configuration files." );
        }
        if ( yamlFile.exists())
        {
            result = loadYamlFile( yamlFile );
            logger.debug ("Read yaml file containing {}.", result);
        }
        else if ( propertyFile.exists() )
        {
            result = loadPropertiesFile( propertyFile );
            logger.debug ("Read properties file containing {}.", result);
        }
        return result;
    }


    private Properties loadYamlFile ( final File configFile ) throws ManipulationException
    {
        Properties result = new Properties( );
        Representer representer = new Representer();

        representer.getPropertyUtils().setSkipMissingProperties(true);
        Yaml yaml = new Yaml( representer);

        try
        {
            YamlFile yf = yaml.loadAs( new FileInputStream( configFile ), YamlFile.class);
            result.putAll( yf.getPme() );
        }
        catch ( FileNotFoundException e )
        {
            throw new ManipulationException( "Unable to load yaml file.", e);
        }
        return result;
    }

    private Properties loadPropertiesFile( final File configFile ) throws ManipulationException
    {
        Properties result = new Properties( );

        try (InputStream input = new FileInputStream( configFile ) )
        {
            result.load( input );
        }
        catch ( IOException e )
        {
            throw new ManipulationException( "Unable to load properties file.", e);
        }
        return result;
    }
}
