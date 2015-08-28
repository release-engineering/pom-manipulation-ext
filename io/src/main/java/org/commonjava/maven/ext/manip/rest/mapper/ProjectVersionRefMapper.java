/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.commonjava.maven.ext.manip.rest.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.ObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
@SuppressWarnings( "unchecked" )
public class ProjectVersionRefMapper implements ObjectMapper
{

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper
        = new com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public Map<ProjectVersionRef, String> readValue( String s )
    {
        List<Map<String, Object>> responseBody;
        Map<ProjectVersionRef, String> result = new HashMap<ProjectVersionRef, String>();

        try
        {
            responseBody = objectMapper.readValue( s, List.class );
        }
        catch ( IOException e )
        {
            throw new RestException( e.getMessage() );
        }

        for ( Map<String, Object> gav: responseBody )
        {
            String groupId = (String) gav.get( "groupId" );
            String artifactId = (String) gav.get( "artifactId" );
            String version = (String) gav.get( "version" );
            String bestMatchVersion = (String) gav.get( "bestMatchVersion" );

            if ( bestMatchVersion != null )
            {
                ProjectVersionRef project = new ProjectVersionRef( groupId, artifactId, version );
                result.put( project, bestMatchVersion );
            }
        }

        return result;
    }

    @Override
    public String writeValue( Object value )
    {
        List<ProjectVersionRef> projects = (List<ProjectVersionRef>) value;

        List requestBody = new ArrayList();
        for ( ProjectVersionRef project : projects )
        {
            Map<String, Object> gav = new HashMap<String, Object>();
            gav.put( "groupId", project.getGroupId() );
            gav.put( "artifactId", project.getArtifactId() );
            gav.put( "version", project.getVersionString() );

            requestBody.add( gav );
        }

        try
        {
            return objectMapper.writeValueAsString( requestBody );
        }
        catch ( JsonProcessingException e )
        {
            throw new RestException( e.getMessage() );
        }
    }
}
