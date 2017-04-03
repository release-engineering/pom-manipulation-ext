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

import org.commonjava.maven.ext.manip.impl.RepoAndReportingRemovalManipulator;

import java.util.Properties;

/**
 * Captures configuration relating to removing reporting/repositories from the POMs. Used by {@link RepoAndReportingRemovalManipulator}.
 *
 */
public class RepoReportingState
    implements State
{
    private static final String RR_SUFFIX_SYSPROP = "repo-reporting-removal";

    private static final String RR_SUFFIX_SYSPROP_LOCAL = "repo-removal-ignorelocalhost";

    /**
     * Default value is off.<br/>
     * It can be overridden to:
     * <br/>'' (empty) which means disabled
     * <br/>'settings.xml' which implicitly means the current build directory
     * <br/>'filename' which should be a valid path to write to
     */
    private static final String RR_SETTINGS_SFX_SYSPROP = "repo-removal-backup";

    private final boolean removal;

    private final String settings;

    private final boolean ignoreLocal;

    public RepoReportingState( final Properties userProps )
    {
        removal = Boolean.parseBoolean( userProps.getProperty( RR_SUFFIX_SYSPROP ) );

        ignoreLocal = Boolean.parseBoolean( userProps.getProperty( RR_SUFFIX_SYSPROP_LOCAL) );

        settings = userProps.getProperty( RR_SETTINGS_SFX_SYSPROP );
    }

    /**
     * Enabled ONLY if repo-reporting-removal is provided in the user properties / CLI -D options.
     *
     * @see #RR_SUFFIX_SYSPROP
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return removal;
    }

    public String getRemovalBackupSettings()
    {
        return settings;
    }

    public boolean ignoreLocal()
    {
        return ignoreLocal;
    }
}
