/*
 * Copyright (C) 2012 Red Hat, Inc.
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

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.ext.annotation.ConfigValue;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.impl.DependencyManipulator;
import org.commonjava.maven.ext.io.rest.DefaultTranslator;
import org.commonjava.maven.ext.io.rest.Translator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Captures configuration relating to dependency alignment from the POMs. Used by {@link DependencyManipulator}.
 */
public class RESTState implements State
{
    @ConfigValue( docIndex = "dep-manip.html#rest-endpoint" )
    public static final String REST_URL = "restURL";

    @ConfigValue( docIndex = "dep-manip.html#rest-endpoint" )
    public static final String REST_REPO_GROUP = "restRepositoryGroup";

    @ConfigValue( docIndex = "dep-manip.html#rest-endpoint" )
    public static final String REST_MAX_SIZE = "restMaxSize";

    @ConfigValue( docIndex = "dep-manip.html#rest-endpoint" )
    public static final String REST_MIN_SIZE = "restMinSize";

    @ConfigValue( docIndex = "dep-manip.html#rest-endpoint" )
    public static final String REST_SUFFIX = "restSuffixAlign";

    @ConfigValue( docIndex = "dep-manip.html#rest-endpoint" )
    public static final String REST_HEADERS = "restHeaders";

    private final ManipulationSession session;

    private String restURL;

    private Translator restEndpoint;

    private boolean restSuffixAlign;

    private Map<String, String> restHeaders;

    public RESTState( final ManipulationSession session )
    {
        this.session = session;

        initialise( session.getUserProperties() );
    }

    public void initialise( Properties userProps )
    {
        final VersioningState vState = session.getState( VersioningState.class );

        restURL = userProps.getProperty( REST_URL );

        String repositoryGroup = userProps.getProperty( REST_REPO_GROUP, "" );
        int restMaxSize = Integer.parseInt( userProps.getProperty( REST_MAX_SIZE, "-1" ) );
        int restMinSize = Integer.parseInt( userProps.getProperty( REST_MIN_SIZE,
                                                                   String.valueOf( DefaultTranslator.CHUNK_SPLIT_COUNT ) ) );
        restSuffixAlign = Boolean.parseBoolean( userProps.getProperty( REST_SUFFIX, "true" ) );

        String restHeadersProperty = userProps.getProperty( REST_HEADERS, "" );
        if ( !StringUtils.isEmpty( restHeadersProperty ) )
        {
            restHeaders = Arrays.stream( restHeadersProperty.split( "," ) )
                                .map( h -> h.split( ":", 2 ) )
                                .filter( h -> h.length > 0 && StringUtils.isNotEmpty( h[0] ) )
                                .collect( Collectors.toMap( h -> h[0], h -> h.length > 1 ? h[1] : "",
                                                            ( x, y ) -> y, LinkedHashMap::new ) );
        }

        restEndpoint = new DefaultTranslator( restURL, restMaxSize, restMinSize, repositoryGroup, vState.getIncrementalSerialSuffix(), restHeaders );
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

    public boolean isRestSuffixAlign()
    {
        return restSuffixAlign;
    }
}
