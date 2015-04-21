/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.ext.manip.impl.ProjectVersionEnforcingManipulator;

import java.util.Properties;

/**
 * Captures configuration relating to enforcing removal of use of ${project.version} from pom. {@link ProjectVersionEnforcingManipulator}.
 */
public class ProjectVersionEnforcingState
    implements State
{
    /**
     * Property used to set the enforcement mode.
     */
    public static final String ENFORCE_SYSPROP = "enforce-project-version";

    // Default to on.
    private final boolean mode;

    public ProjectVersionEnforcingState( final Properties userProps )
    {
        final String value = userProps.getProperty( ENFORCE_SYSPROP );
        if ( value != null)
        {
            mode = Boolean.parseBoolean( userProps.getProperty( ENFORCE_SYSPROP ) );
        }
        else
        {
            mode = true;
        }
    }

    /**
     * Enabled by default; may be disabled by enforce-project-version.
     *
     * @see #ENFORCE_SYSPROP
     * @see State#isEnabled()
     * @see EnforcingMode
     */
    @Override
    public boolean isEnabled()
    {
        return mode;
    }
}
