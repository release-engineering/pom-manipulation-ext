/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.DependencyManipulator;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Captures configuration relating to dependency alignment from the POMs. Used by {@link DependencyManipulator}.
 */
public class DependencyRESTState extends CommonDependencyState
    implements State
{
    /**
     * The name of the property which contains the remote REST URL.
     */
    public static final String REST_URL = "restEndpoint";

    private final String restURL;

    private final Map<ProjectVersionRef, String> projectVersionsByGAV = new HashMap<ProjectVersionRef, String>();


    public DependencyRESTState( final Properties userProps )
    {
        super (userProps);

        restURL = userProps.getProperty( "restURL" );
    }

    /**
     * Enabled ONLY if propertyManagement is provided in the user properties / CLI -D options.
     *
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return restURL != null && restURL.length() > 0;
    }

    /**
     * @return value of REST URL.
     */
    public String getRESTURL()
    {
        return restURL;
    }
}
