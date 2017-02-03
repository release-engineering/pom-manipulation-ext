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

import java.util.Properties;

/**
 * Captures configuration parameters for use with {@link org.commonjava.maven.ext.manip.impl.BOMBuilderManipulator}.
 */
public class BOMInjectingState
    implements State
{

    /** Set this property to true using <code>-DbomBuilder=true</code> to activate BOM Builder Plugin */
    private static final String BOM_BUILDER = "bomBuilder";

    private final boolean builderEnabled;

    /**
     * Detects whether this state is enabled..
     *
     * @param userProperties the properties for the manipulator
     */
    public BOMInjectingState( final Properties userProperties )
    {
        builderEnabled = Boolean.parseBoolean( userProperties.getProperty( BOM_BUILDER, "false" ) );
    }

    /**
     * @see BOMInjectingState#BOM_BUILDER
     *
     * @return true if state is enabled.
     */
    @Override
    public boolean isEnabled()
    {
        return builderEnabled;
    }

}
