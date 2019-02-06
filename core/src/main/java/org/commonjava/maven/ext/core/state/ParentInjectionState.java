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
package org.commonjava.maven.ext.core.state;

import org.apache.maven.model.Parent;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.core.impl.RepositoryInjectionManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Captures configuration relating to injection of parent.
 * Used by {@link RepositoryInjectionManipulator}.
 */
public class ParentInjectionState
    implements State
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Suffix to enable this modder
     */
    private static final String PARENT_INJECTION_PROPERTY = "parentInjection";

    private final Parent parent;

    public ParentInjectionState( final Properties userProps )
    {
        final String gav = userProps.getProperty( PARENT_INJECTION_PROPERTY );

        if ( gav != null )
        {
            ProjectVersionRef ref = SimpleProjectVersionRef.parse( gav );
            parent = new Parent();
            parent.setGroupId( ref.getGroupId() );
            parent.setArtifactId( ref.getArtifactId() );
            parent.setVersion( ref.getVersionString() );
            parent.setRelativePath( "" );
        }
        else
        {
            parent = null;
        }
    }

    /**
     * Enabled ONLY if parentInjection is provided in the user properties / CLI -D options.
     *
     * @see #PARENT_INJECTION_PROPERTY
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return parent != null;
    }

    public Parent getParentInjection()
    {
        return parent;
    }
}

