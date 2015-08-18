package org.commonjava.maven.ext.rest;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.rest.exception.ClientException;
import org.commonjava.maven.ext.rest.exception.ServerException;
import org.commonjava.maven.ext.rest.exception.RestException;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author vdedik@redhat.com
 */
public class DefaultVersionTranslator implements VersionTranslator {
    private String endpointUrl;

    public DefaultVersionTranslator(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    @SuppressWarnings("unchecked")
    public List<ProjectVersionRef> translateVersions(ProjectVersionRef project, List<ProjectVersionRef> dependencies) {
        List<ProjectVersionRef> result = new ArrayList<ProjectVersionRef>();

        // Prepare request body map
        final String rawProject = project.toString();
        final List<String> rawDependencies = new ArrayList<String>();
        for (ProjectVersionRef dep : dependencies) {
            rawDependencies.add(dep.toString());
        }
        Map<String, Object> requestBodyMap = new HashMap<String, Object>() {{
            put("project", rawProject);
            put("dependencies", rawDependencies);
        }};

        // Execute request to get translated versions
        HttpResponse<JsonNode> r;
        try {
            r = Unirest.post(this.endpointUrl)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(new JSONObject(requestBodyMap).toString())
                    .asJson();
        } catch (UnirestException e) {
            throw new RestException(String.format(
                    "Request to server '%s' failed. Exception message: %s", this.endpointUrl, e.getMessage()));
        }

        // Handle some corner cases (5xx, 4xx)
        if (r.getStatus() / 100 == 5) {
            throw new ServerException(String.format("Server at '%s' failed to translate versions for " +
                    "project '%s'. HTTP status code %s.", this.endpointUrl, rawProject, r.getStatus()));
        } else if (r.getStatus() / 100 == 4) {
            throw new ClientException(String.format("Server at '%s' could not translate versions for " +
                    "project '%s'. HTTP status code %s.", this.endpointUrl, rawProject, r.getStatus()));
        }

        // Get result object from response
        JSONObject jsonResult = r.getBody().getObject();

        // Parse project version and create ProjectVersionRef from it
        ProjectVersionRef projectResult = Utils.fromString(jsonResult.getString("project"));
        result.add(projectResult);

        // Parse dependencies and create ProjectVersionRefs from it
        JSONArray dependenciesGavs = (JSONArray) jsonResult.get("dependencies");
        for (Integer i = 0; i < dependenciesGavs.length(); i++) {
            ProjectVersionRef depResult = Utils.fromString(dependenciesGavs.getString(i));
            result.add(depResult);
        }

        return result;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }
}
