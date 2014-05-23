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

import java.util.Properties;

import org.commonjava.maven.ext.manip.impl.ProfileInjectionManipulator;

/**
 * Captures configuration relating to injection profiles from a remote POM.
 * Used by {@link ProfileInjectionManipulator}.
 */
public class ProfileInjectionState
    implements State
{
    /**
     * Suffix to enable this modder
     */
    public static final String PROFILE_INJECTION_PROPERTY = "profileInjection";

    private final String profileMgmt;

    public ProfileInjectionState( final Properties userProps )
    {
        profileMgmt = userProps.getProperty( PROFILE_INJECTION_PROPERTY );
    }

    /**
     * Enabled ONLY if repo-reporting-removal is provided in the user properties / CLI -D options.
     *
     * @see #PROFILE_INJECTION_PROPERTY
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return profileMgmt != null && profileMgmt.length() > 0;
    }


    public String getRemoteProfileInjectionMgmt()
    {
        return profileMgmt;
    }
}
