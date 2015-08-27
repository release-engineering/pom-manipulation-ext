package org.commonjava.maven.ext.manip.rest;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.ClientException;
import org.commonjava.maven.ext.manip.rest.exception.ServerException;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.json.JSONArray;
import org.json.JSONObject;

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
    }

    @SuppressWarnings( "unchecked" )
    public Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> projects )
    {
        Map<ProjectVersionRef, String> result = new HashMap<ProjectVersionRef, String>();

        // Prepare request body map
        JSONArray requestBody = new JSONArray();
        for ( ProjectVersionRef project : projects )
        {
            JSONObject gav = new JSONObject();
            gav.put( "groupId", project.getGroupId() );
            gav.put( "artifactId", project.getArtifactId() );
            gav.put( "version", project.getVersionString() );

            requestBody.put( gav );
        }

        // Execute request to get translated versions
        HttpResponse<JsonNode> r;
        try
        {
            r = Unirest.post( this.endpointUrl )
                       .header( "accept", "application/json" )
                       .header( "Content-Type", "application/json" )
                       .body( requestBody.toString() )
                       .asJson();
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
                String.format( "Server at '%s' failed to translate versions. " + "HTTP status code %s.",
                               this.endpointUrl, r.getStatus() ) );
        }
        else if ( r.getStatus() / 100 == 4 )
        {
            throw new ClientException(
                String.format( "Server at '%s' could not translate versions. " + "HTTP status code %s.",
                               this.endpointUrl, r.getStatus() ) );
        }

        // Get result object from response
        JSONArray jsonResult = r.getBody().getArray();

        // Populate map with projects and best matching versions
        for ( Integer i = 0; i < jsonResult.length(); i++ )
        {
            String groupId = jsonResult.getJSONObject( i ).getString( "groupId" );
            String artifactId = jsonResult.getJSONObject( i ).getString( "artifactId" );
            String version = jsonResult.getJSONObject( i ).getString( "version" );
            String bestMatchVersion = jsonResult.getJSONObject( i ).getString( "bestMatchVersion" );

            if ( bestMatchVersion != null )
            {
                ProjectVersionRef project = new ProjectVersionRef( groupId, artifactId, version );
                result.put( project, bestMatchVersion );
            }
        }

        return result;
    }

    public String getEndpointUrl()
    {
        return endpointUrl;
    }
}
