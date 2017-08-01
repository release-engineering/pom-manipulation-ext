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
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.ext.manip.util.PropertiesUtils;
import org.commonjava.maven.ext.manip.util.PropertyFlag;

import java.util.Properties;

/**
 * Captures configuration relating to enforcing removal of use of ${project.version} from pom.
 */
public class ProjectVersionEnforcingState
    implements State
{
    /**
     * Property used to set the enforcement mode.
     */
    private static final PropertyFlag
                    ENFORCE_PROJECT_VERSION = new PropertyFlag ( "enforce-project-version", "enforceProjectVersion" );

    static
    {
        State.activeByDefault.add( ProjectVersionEnforcingState.class );
    }

    // Default to on.
    private boolean mode = true;

    public ProjectVersionEnforcingState( final Properties userProps )
    {
        final String value = PropertiesUtils.handleDeprecatedProperty ( userProps, ENFORCE_PROJECT_VERSION );
        if ( value != null )
        {
            mode = Boolean.parseBoolean( value );
        }
        else
        {
            mode = true;
        }
    }

    /**
     * Enabled by default; may be disabled by enforce-project-version.
     *
     * @see #ENFORCE_PROJECT_VERSION
     * @see State#isEnabled()
     * @see EnforcingMode
     */
    @Override
    public boolean isEnabled()
    {
        return mode;
    }
}
