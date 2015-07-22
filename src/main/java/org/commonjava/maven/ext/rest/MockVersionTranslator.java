package org.commonjava.maven.ext.rest;

import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Pattern;

import static com.github.restdriver.clientdriver.RestClientDriver.*;

/**
 * @author vdedik@redhat.com
 */
public class MockVersionTranslator extends DefaultVersionTranslator {

    public static final String MOCK_URL = "http://127.0.0.1:59090";

    public MockVersionTranslator() {
        super(MOCK_URL);

        // Rules for client driver:
        ClientDriver clientDriver = new ClientDriverFactory().createClientDriver(59090);

        JSONArray rawProjects = new JSONArray(Utils.readFileFromClasspath("responses.json"));
        for (Integer i = 0; i < rawProjects.length(); i++) {
            JSONObject rawProject = rawProjects.getJSONObject(i);
            String projectNameWoSfx = rawProject.getString("project").replaceAll("[.-]redhat-\\d", "");
            Pattern pattern = Utils.getProjectMatcher(projectNameWoSfx);
            clientDriver.addExpectation(onRequestTo("/").withMethod(Method.POST).withBody(pattern, "application/json"),
                    giveResponse(rawProject.toString(), "application/json"));
        }
    }
}
