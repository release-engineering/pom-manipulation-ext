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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.DependencyManipulator;
import org.commonjava.maven.ext.manip.util.IdUtils;

import java.util.List;
import java.util.Properties;

/**
 * Captures configuration relating to dependency alignment from the POMs. Used by {@link DependencyManipulator}.
 */
public class DependencyState extends CommonDependencyState
    implements State
{
    /**
     * Two possible formats currently supported for version property output:
     * <ul>
     * <li><code>version.group</code></li>
     * <li><code>version.group.artifact</code></li>
     * <li><code>none</code> (equates to off)</li>
     * </ul>
     * Configured by the property <code>-DversionPropertyFormat=[VG|VGA|NONE]</code>
     */
    public static enum VersionPropertyFormat
    {
        VG,
        VGA,
        NONE;
    }

    /**
     * The name of the property which contains the GAV of the remote pom from which to retrieve dependency management
     * information.
     * <pre>
     * <code>-DdependencyManagement:org.foo:bar-dep-mgmt:1.0</code>
     * </pre>
     */
    public static final String DEPENDENCY_MANAGEMENT_POM_PROPERTY = "dependencyManagement";

    private final List<ProjectVersionRef> depMgmt;

    public DependencyState( final Properties userProps )
    {
        super (userProps);

        depMgmt = IdUtils.parseGAVs( userProps.getProperty( DEPENDENCY_MANAGEMENT_POM_PROPERTY ) );
    }

    /**
     * Enabled ONLY if propertyManagement is provided in the user properties / CLI -D options.
     *
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return depMgmt != null && !depMgmt.isEmpty();
    }

    public List<ProjectVersionRef> getRemoteDepMgmt()
    {
        return depMgmt;
    }
}
