/**
 *  Copyright (C) 2016 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Properties;

/**
 * Captures configuration relating to groupId relocation for POMs.
 */
public class RelocationState
    implements State
{
    /**
     * The name of the property which contains a comma separated list of groupId and optional version relocations to perform.
     *
     * <pre>
     * <code>-DdependencyRelocation=oldGroupId:newGroupId@version,....</code>
     * </pre>
     */
    public static final String DEPENDENCY_RELOCATIONS = "dependencyRelocations";

    private static final Logger logger = LoggerFactory.getLogger( RelocationState.class );

    /**
     * GroupId:ProjectVersionRef (contains newGroupdId:*:newVersion)
     */
    private final HashMap<String, ProjectVersionRef> dependencyRelocations = new HashMap<String, ProjectVersionRef>(  );

    public RelocationState( final Properties userProps )
                    throws ManipulationException
    {
        String value = userProps.getProperty( DEPENDENCY_RELOCATIONS );

        if ( !( value == null || value.length () == 0) )
        {
            final String[] relocations = value.split( "," );
            for ( final String r : relocations )
            {
                final String[] relocation = r.split( ":" );
                if (relocation.length != 2)
                {
                    throw new ManipulationException( "Invalid format for DependencyRelocations: {}", relocation );
                }
                try
                {
                    ProjectVersionRef ref;
                    if (relocation[1].contains( "@" ))
                    {
                        String [] gv = relocation[1].split( "@" );
                        if (gv.length != 2)
                        {
                            throw new ManipulationException( "Invalid format for DependencyRelocations : {}", relocation );
                        }
                        ref = new SimpleProjectVersionRef ( gv[0], "*", gv[1] );
                    }
                    else
                    {
                        ref = new SimpleProjectVersionRef( relocation[1], "*", "*" );
                    }
                    dependencyRelocations.put( relocation[0], ref);
                }
                catch ( final InvalidRefException e )
                {
                    logger.error( "Error building ProjectVersionRef for dependencyRelocations: " + r, e );
                    throw e;
                }
            }
        }
    }

    /**
     * Enabled ONLY if dependencyRelocation is provided in the user properties / CLI -D options.
     *
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return dependencyRelocations != null && !dependencyRelocations.isEmpty();
    }

    public HashMap<String, ProjectVersionRef> getDependencyRelocations()
    {
        return dependencyRelocations;
    }
}
