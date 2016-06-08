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
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.ProfileInjectionManipulator;
import org.commonjava.maven.ext.manip.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

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
    private static final String PROFILE_INJECTION_PROPERTY = "profileInjection";

    /**
     */
    private static final String PROFILE_INJECTION_GA = "profileInjectionPoms";

    private final ProjectVersionRef profileMgmt;

    private final List<ProjectRef> groupArtifact;

    public ProfileInjectionState( final Properties userProps )
    {
        groupArtifact = IdUtils.parseGAs( userProps.getProperty( PROFILE_INJECTION_GA ) );

        final String gav = userProps.getProperty( PROFILE_INJECTION_PROPERTY );
        ProjectVersionRef ref = null;
        if ( gav != null && !gav.isEmpty())
        {
            try
            {
                ref = SimpleProjectVersionRef.parse( gav );
            }
            catch ( final InvalidRefException e )
            {
                logger.error( "Skipping profile injection! Got invalid profileInjection GAV: {}", gav );
                throw e;
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

    public List<ProjectRef> getRemoteProfileInjectionTargets()
    {
        return groupArtifact;
    }

    public ProjectVersionRef getRemoteProfileInjectionMgmt()
    {
        return profileMgmt;
    }
}
