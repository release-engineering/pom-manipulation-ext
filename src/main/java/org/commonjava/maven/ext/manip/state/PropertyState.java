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

import java.util.List;
import java.util.Properties;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.AlignmentManipulator;
import org.commonjava.maven.ext.manip.impl.PropertyManipulator;
import org.commonjava.maven.ext.manip.util.IdUtils;

/**
 * Captures configuration relating to property injection from the POMs. Used by {@link PropertyManipulator}
 * and {@link AlignmentManipulator}s.
 *
 */
public class PropertyState
    implements State
{

    /**
     * Suffix to enable this modder. The name of the property which contains the GAV of the remote pom from
     * which to retrieve property mapping information. <br />
     * <code>-DpropertyManagement:org.foo:bar-property-mgmt:1.0</code>
     */
    public static final String PROPERTY_MANAGEMENT_POM_PROPERTY = "propertyManagement";

    private final List<ProjectVersionRef> propertyMgmt;

    public PropertyState( final Properties userProps )
    {
        propertyMgmt = IdUtils.parseGAVs( userProps.getProperty( PROPERTY_MANAGEMENT_POM_PROPERTY ) );
    }

    /**
     * Enabled ONLY if propertyManagement is provided in the user properties / CLI -D options.
     *
     * @see #ENFORCE_SYSPROP
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
