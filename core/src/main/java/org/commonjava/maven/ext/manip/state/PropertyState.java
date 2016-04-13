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
import org.commonjava.maven.ext.manip.impl.PropertyManipulator;
import org.commonjava.maven.ext.manip.util.IdUtils;

import java.util.List;
import java.util.Properties;

/**
 * Captures configuration relating to property injection from the POMs. Used by {@link PropertyManipulator}.
 *
 */
public class PropertyState
    implements State
{

    /**
     * Suffix to enable this modder. The name of the property which contains the GAV of the remote pom from
     * which to retrieve property mapping information.
     * <pre>
     * <code>-DpropertyManagement:org.foo:bar-property-mgmt:1.0</code>
     * </pre>
     */
    private static final String PROPERTY_MANAGEMENT_POM_PROPERTY = "propertyManagement";

    private final List<ProjectVersionRef> propertyMgmt;

    public PropertyState( final Properties userProps )
    {
        propertyMgmt = IdUtils.parseGAVs( userProps.getProperty( PROPERTY_MANAGEMENT_POM_PROPERTY ) );
    }

    /**
     * Enabled ONLY if propertyManagement is provided in the user properties / CLI -D options.
     *
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return propertyMgmt != null && !propertyMgmt.isEmpty();
    }

    public List<ProjectVersionRef> getRemotePropertyMgmt()
    {
        return propertyMgmt;
    }
}
