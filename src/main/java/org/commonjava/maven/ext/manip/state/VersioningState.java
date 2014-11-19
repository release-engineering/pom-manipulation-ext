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

import java.util.Map;
import java.util.Properties;

import org.commonjava.maven.ext.manip.impl.ProjectVersioningManipulator;

/**
 * Captures configuration and changes relating to the projects' versions. Used by {@link ProjectVersioningManipulator}.
 *
 * @author jdcasey
 */
public class VersioningState
    implements State
{

    public static final String VERSION_SUFFIX_SYSPROP = "version.suffix";

    public static final String INCREMENT_SERIAL_SUFFIX_SYSPROP = "version.incremental.suffix";

    public static final String VERSION_SUFFIX_SNAPSHOT_SYSPROP = "version.suffix.snapshot";

    private Map<String, String> versioningChanges;

    private final String suffix;

    private final String incrementSerialSuffix;

    private final boolean preserveSnapshot;

    public VersioningState( final Properties userProps )
    {
        suffix = userProps.getProperty( VERSION_SUFFIX_SYSPROP );
        incrementSerialSuffix = userProps.getProperty( INCREMENT_SERIAL_SUFFIX_SYSPROP );
        preserveSnapshot = Boolean.parseBoolean( userProps.getProperty( VERSION_SUFFIX_SNAPSHOT_SYSPROP ) );
    }

    public void setVersioningChanges( final Map<String, String> versioningChanges )
    {
        this.versioningChanges = versioningChanges;
    }

    public Map<String, String> getVersioningChanges()
    {
        return versioningChanges;
    }

    public String getIncrementalSerialSuffix()
    {
        return incrementSerialSuffix;
    }

    public String getSuffix()
    {
        return suffix;
    }

    public boolean preserveSnapshot()
    {
        return preserveSnapshot;
    }

    /**
     * Enabled ONLY if either version.incremental.suffix or version.suffix is provided in the user properties / CLI -D options.
     *
     * @see #VERSION_SUFFIX_SYSPROP
     * @see #INCREMENT_SERIAL_SUFFIX_SYSPROP
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return incrementSerialSuffix != null || suffix != null;
    }

}
