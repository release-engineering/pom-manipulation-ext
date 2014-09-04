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

import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.ProfileInjectionManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures configuration relating to injection profiles from a remote POM.
 * Used by {@link ProfileInjectionManipulator}.
 */
public class ProfileInjectionState
    implements State
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Suffix to enable this modder
     */
    public static final String PROFILE_INJECTION_PROPERTY = "profileInjection";

    private final ProjectVersionRef profileMgmt;

    public ProfileInjectionState( final Properties userProps )
    {
        final String gav = userProps.getProperty( PROFILE_INJECTION_PROPERTY );
        ProjectVersionRef ref = null;
        if ( gav != null )
        {
            try
            {
                ref = ProjectVersionRef.parse( gav );
            }
            catch ( final InvalidRefException e )
            {
                logger.warn( "Skipping profile injection! Got invalid profileInjection GAV: {}", gav );
            }
        }

        profileMgmt = ref;
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
        return profileMgmt != null;
    }


    public ProjectVersionRef getRemoteProfileInjectionMgmt()
    {
        return profileMgmt;
    }
}
