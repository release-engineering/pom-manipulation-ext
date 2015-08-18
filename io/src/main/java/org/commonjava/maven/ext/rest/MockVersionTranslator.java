package org.commonjava.maven.ext.rest;

import org.commonjava.maven.ext.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.server.HttpServer;
import org.commonjava.maven.ext.server.JettyHttpServer;

/**
 * @author vdedik@redhat.com
 */
public class MockVersionTranslator extends DefaultVersionTranslator {
    public static final Integer PORT = 8090;
    public static final String MOCK_URL = "http://127.0.0.1:" + PORT;

    private final HttpServer jettyServer;

    public MockVersionTranslator() {
        super(MOCK_URL);
        jettyServer = new JettyHttpServer(new AddSuffixJettyHandler(), PORT);
    }

    public void shutdownMockServer() {
        jettyServer.shutdown();
    }
}
