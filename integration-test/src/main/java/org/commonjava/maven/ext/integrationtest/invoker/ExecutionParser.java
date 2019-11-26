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
package org.commonjava.maven.ext.integrationtest.invoker;

import org.apache.commons.lang.SystemUtils;
import org.commonjava.maven.ext.integrationtest.TestUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author vdedik@redhat.com
 */
public interface ExecutionParser
{
    ExecutionParserHandler SKIP_HANDLER = new ExecutionParserHandler()
    {
        @Override
        public void handle( Execution execution, Map<String, String> params )
        {
            String key = params.get( "key" );
            String value = params.get( "value" );

            if ( key.matches( "invoker\\.os\\.family.*" ) && value != null)
            {
                value = value.trim();
                boolean invert = false;
                if ( value.startsWith( "!" ) )
                {
                    invert = true;
                    value = value.substring( 1 );
                }
                // TODO: Very limited handling of selector conditions - just enough to skip for Windows.
                if ( "windows".equalsIgnoreCase( value ) )
                {
                    if ( invert && SystemUtils.IS_OS_WINDOWS )
                    {
                        execution.setSkip( true );
                    }
                }
            }
        }
    };

    /**
     * Sets mvn command of Execution
     */
    ExecutionParserHandler BUILD_HANDLER = new ExecutionParserHandler()
    {
        @Override
        public void handle( Execution execution, Map<String, String> params )
        {
            String key = params.get( "key" );
            String value = params.get( "value" );

            if ( key.matches( "invoker\\.goals.*" ) )
            {
                execution.setMvnCommand( value );
            }
            else if ( !key.matches( "invoker\\..*" ) && value.isEmpty() )
            {
                execution.setMvnCommand( key );
            }
        }
    };

    ExecutionParserHandler BUILD_PROFILES_HANDLER = new ExecutionParserHandler()
    {
        @Override
        public void handle( Execution execution, Map<String, String> params )
        {
            String key = params.get( "key" );
            String value = params.get( "value" );

            if ( key.matches( "invoker\\.profiles.*" ) )
            {
                Map<String,String> flags = execution.getFlags();
                if ( flags == null)
                {
                    flags = new HashMap<>(  );
                }
                flags.put( "activeProfiles", value );
                execution.setFlags( flags );
            }
        }
    };

    /**
     * Sets expected result of Execution (i.e. success true/false)
     */
    ExecutionParserHandler BUILD_RESULT_HANDLER = new ExecutionParserHandler()
    {
        @Override
        public void handle( Execution execution, Map<String, String> params )
        {
            String key = params.get( "key" );
            String value = params.get( "value" );

            if ( key.matches( "invoker\\.buildResult.*" ) && value.equals( "failure" ) )
            {
                execution.setSuccess( false );
            }
        }
    };

    /**
     * Sets java parameters to Execution
     */
    ExecutionParserHandler SYSTEM_PROPERTIES_HANDLER = new ExecutionParserHandler()
    {
        @Override
        public void handle( Execution execution, Map<String, String> params )
        {
            String key = params.get( "key" );
            String value = params.get( "value" );

            if ( key.matches( "invoker\\.systemPropertiesFile.*" ) )
            {
                Properties props = TestUtils.loadProps( execution.getLocation() + "/" + value );

                // Properties to Map
                Map<String, String> javaParams = TestUtils.propsToMap( props );
                if ( execution.getJavaParams() != null )
                {
                    javaParams.putAll( execution.getJavaParams() );
                }

                // And put it all into execution
                execution.setJavaParams( javaParams );
            }
        }
    };

    /**
     * Used after all other handlers were run
     */
    ExecutionParserHandler POST_HANDLER = new ExecutionParserHandler()
    {
        @Override
        public void handle( Execution execution, Map<String, String> params )
        {
            if ( execution.getJavaParams() == null )
            {
                Properties props = TestUtils.loadProps( execution.getLocation() + "/test.properties" );
                Map<String, String> javaParams = TestUtils.propsToMap( props );
                execution.setJavaParams( javaParams );
            }
        }
    };

    /**
     * Parse invoker properties from workingDir into a collection of executions.
     *
     * @param workingDir - Working directory where invoker.properties are present
     * @return Collection of executions
     */
    Collection<Execution> parse( String workingDir );

    /**
     * Adds another execution parser handler (to be used when parsing)
     *
     * @param handler - Execution parser handler
     */
    void addHandler( ExecutionParserHandler handler );
}
