package org.commonjava.maven.ext.rest;

import com.github.restdriver.clientdriver.ClientDriverRequest.Method;
import com.github.restdriver.clientdriver.ClientDriver;
import com.github.restdriver.clientdriver.ClientDriverFactory;

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

        Pattern pattern = Utils.getProjectMatcher("org.overlord.rtgov:parent:2.0.2");
        clientDriver.addExpectation(onRequestTo("/").withMethod(Method.POST).withBody(pattern, "application/json"),
                giveResponse(Utils.readFileFromClasspath("rtgov-response.json"), "application/json"));
    }
}
