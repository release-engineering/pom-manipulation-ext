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
package org.commonjava.maven.ext.manip.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.ClientException;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.exception.ServerException;
import org.commonjava.maven.ext.manip.rest.mapper.ProjectVersionRefMapper;

import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public class DefaultVersionTranslator
    implements VersionTranslator
{
    private String endpointUrl;

    public DefaultVersionTranslator( String endpointUrl )
    {
        this.endpointUrl = endpointUrl;
        Unirest.setObjectMapper( new ProjectVersionRefMapper() );
    }

    @SuppressWarnings( "unchecked" )
    public Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> projects )
    {
        // Execute request to get translated versions
        HttpResponse<Map> r;
        try
        {
            r = Unirest.post( this.endpointUrl )
                       .header( "accept", "application/json" )
                       .header( "Content-Type", "application/json" )
                       .body( projects )
                       .asObject( Map.class );
        }
        catch ( UnirestException e )
        {
            throw new RestException(
                String.format( "Request to server '%s' failed. Exception message: %s", this.endpointUrl,
                               e.getMessage() ), e );
        }

        // Handle some corner cases (5xx, 4xx)
        if ( r.getStatus() / 100 == 5 )
        {
            throw new ServerException(
                String.format( "Server at '%s' failed to translate versions. HTTP status code %s.",
                               this.endpointUrl, r.getStatus() ) );
        }
        else if ( r.getStatus() / 100 == 4 )
        {
            throw new ClientException(
                String.format( "Server at '%s' could not translate versions. HTTP status code %s.",
                               this.endpointUrl, r.getStatus() ) );
        }

        return r.getBody();
    }

    public String getEndpointUrl()
    {
        return endpointUrl;
    }
}
