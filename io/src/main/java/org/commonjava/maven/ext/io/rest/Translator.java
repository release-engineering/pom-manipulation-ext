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
package org.commonjava.maven.ext.io.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;

import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public interface Translator
{
    int CHUNK_SPLIT_COUNT = 4;

    long DEFAULT_CONNECTION_TIMEOUT_SEC = 30;

    long DEFAULT_SOCKET_TIMEOUT_SEC = 600;

    int RETRY_DURATION_SEC = 30;
    /**
     * Executes HTTP request to a REST service that translates versions
     *
     * @param projects - List of projects (GAVs)
     * @return Map of ProjectVersionRef objects as keys and translated versions as values
     */
    Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> projects );

    List<ProjectVersionRef> findBlacklisted( ProjectRef project );

    enum RestProtocol
    {
        // These two are equivalent. Keeping current for backwards compatibility.
        CURRENT( "current" );

        private String name;

        RestProtocol( String name )
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }

        public static RestProtocol parse( String protocol ) throws ManipulationException
        {
            for ( RestProtocol r : RestProtocol.values() )
            {
                if ( r.toString().equals( protocol ) )
                {
                    return r;
                }
            }
            throw new ManipulationException( "Unknown protocol " + protocol );
        }
    }
}
