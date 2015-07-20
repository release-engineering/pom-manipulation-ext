package org.commonjava.maven.ext.rest;

import java.util.List;
import java.util.ArrayList;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author vdedik@redhat.com
 */
public class VersionTranslator {
    private String endpointUrl;

    public VersionTranslator(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    /**
     * Executes HTTP request to a REST service that translates versions
     *
     * @param project - Represents project with groupId, artifactId and version
     * @param dependencies - List of dependencies of the project
     * @return List of ProjectVersionRef objects, cointains both the main project and it's dependencies
     */
    @SuppressWarnings("unchecked")
    public List<ProjectVersionRef> translateVersions(ProjectVersionRef project, List<ProjectVersionRef> dependencies) {
        List<ProjectVersionRef> result = new ArrayList<ProjectVersionRef>();

        // Prepare rest parameters
        String rawProject = project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersionString();
        List<String> rawDependencies = new ArrayList<String>();
        for (ProjectVersionRef dep : dependencies) {
            String gav = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersionString();
            rawDependencies.add(gav);
        }

        // Execute request to get translated versions
        HttpResponse<JsonNode> rawResult = null;
        try {
            rawResult = Unirest.post(this.endpointUrl)
                    .header("accept", "application/json")
                    .field("project", rawProject)
                    .field("dependencies", rawDependencies)
                    .asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        // TODO: test some corner cases (404, 500 etc)
        JSONObject jsonResult = rawResult.getBody().getObject();

        // Parse project version and create ProjectVersionRef from it
        String projectGav = (String) jsonResult.get("project");
        String[] projectGavSplit = projectGav.split(":");
        ProjectVersionRef projectResult =
                new ProjectVersionRef(projectGavSplit[0], projectGavSplit[1], projectGavSplit[2]);
        result.add(projectResult);

        // Parse dependencies and create ProjectVersionRefs from it
        JSONArray dependenciesGavs = (JSONArray) jsonResult.get("dependencies");
        for (Integer i = 0; i < dependenciesGavs.length(); i++) {
            String[] depGavSplit = dependenciesGavs.getString(i).split(":");
            ProjectVersionRef depResult = new ProjectVersionRef(depGavSplit[0], depGavSplit[1], depGavSplit[2]);
            result.add(depResult);
        }

        return result;
    }
}
