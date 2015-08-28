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
                               e.getMessage() ) );
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
