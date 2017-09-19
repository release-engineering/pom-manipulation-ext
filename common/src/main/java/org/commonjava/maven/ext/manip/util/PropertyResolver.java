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

import org.apache.maven.model.Profile;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.session.MavenSessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Commonly used manipulations / extractions from project / user (CLI) properties.
 */
public final class PropertyResolver
{
    private final static Logger logger = LoggerFactory.getLogger( PropertyResolver.class );

    private PropertyResolver()
    {
    }

    /**
     * This recursively checks the supplied value and recursively resolves it if its a property.
     *
     * @param session the manipulation session.
     * @param value value to check
     * @return the version string
     * @throws ManipulationException if an error occurs
     */
    public static String resolveInheritedProperties( MavenSessionHandler session, Project start, String value ) throws ManipulationException
    {
        final List<Project> found = new ArrayList<>(  );
        found.add( start );

        Project loop = start;
        while ( loop.getProjectParent() != null)
        {
            // Place inherited first so latter down tree take precedence.
            found.add( 0, loop.getProjectParent() );
            loop = loop.getProjectParent();
        }

        PropertyResolver.logger.debug ( "### Resolving inherited properties for projects {} ", found);
        return PropertyResolver.resolveProperties( session, found, value );
    }

    private static Properties searchProfiles( MavenSessionHandler session, Project p )
    {
        final Properties result = new Properties();

        for ( Profile pr : ProfileUtils.getProfiles( session, p.getModel() ) )
        {
            result.putAll( pr.getProperties() );
        }
        return result;
    }

    /**
     * This recursively checks the supplied value and recursively resolves it if its a property.
     *
     * @param session the current session
     * @param projects set of projects
     * @param value value to check
     * @return the version string
     * @throws ManipulationException if an error occurs
     */
    static String resolveProperties( MavenSessionHandler session, List<Project> projects, String value ) throws ManipulationException
    {
        final Properties amalgamated = new Properties();
        Project executionRoot = null;

        // Save execution root so it can potentially overwrite.
        for ( Project p : projects )
        {
            if ( p.isExecutionRoot() )
            {
                executionRoot = p;
            }
            else
            {
                amalgamated.putAll( p.getModel().getProperties() );
                amalgamated.putAll( searchProfiles( session, p ) );
            }
        }
        // In theory executionRoot should never be null but some artificially constructed unit tests don't define
        // it so lets avoid a null ptr.
        if ( executionRoot != null)
        {
            amalgamated.putAll( executionRoot.getModel().getProperties() );
            amalgamated.putAll( searchProfiles( session, executionRoot ) );
        }

        PropertyInterpolator pi = new PropertyInterpolator( amalgamated, projects.get( 0 ) );
        return pi.interp( value );
    }
}
