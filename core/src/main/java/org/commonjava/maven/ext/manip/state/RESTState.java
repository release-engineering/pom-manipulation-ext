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

import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.impl.DependencyManipulator;
import org.commonjava.maven.ext.manip.rest.DefaultTranslator;
import org.commonjava.maven.ext.manip.rest.Translator;
import org.commonjava.maven.ext.manip.rest.Translator.RestProtocol;

import java.util.Properties;

/**
 * Captures configuration relating to dependency alignment from the POMs. Used by {@link DependencyManipulator}.
 */
public class RESTState implements State
{
    private final String restURL;

    private final Translator restEndpoint;

    public RESTState( final Properties userProps ) throws ManipulationException
    {
        restURL = userProps.getProperty( "restURL" );

        String repositoryGroup = userProps.getProperty( "restRepositoryGroup", "" );
        int restMaxSize = Integer.valueOf( userProps.getProperty( "restMaxSize", "0" ) );
        int restMinSize = Integer.valueOf( userProps.getProperty( "restMinSize",
                                                                  String.valueOf( DefaultTranslator.CHUNK_SPLIT_COUNT ) ) );

        RestProtocol protocol = RestProtocol.parse ( userProps.getProperty( "restProtocol", RestProtocol.CURRENT.toString() ) );

        restEndpoint = new DefaultTranslator( restURL, protocol, restMaxSize, restMinSize, repositoryGroup );
    }

    /**
     * Enabled ONLY if propertyManagement is provided in the user properties / CLI -D options.
     *
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return restURL != null && !restURL.isEmpty();
    }

    public Translator getVersionTranslator()
    {
        return restEndpoint;
    }
}
