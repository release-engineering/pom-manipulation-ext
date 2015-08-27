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

package org.commonjava.maven.ext.manip.rest.rule;

import org.commonjava.maven.ext.manip.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.manip.server.HttpServer;
import org.commonjava.maven.ext.manip.server.JettyHttpServer;
import org.junit.rules.ExternalResource;

/**
 * @author vdedik@redhat.com
 */
public class MockServer
    extends ExternalResource
{

    private HttpServer httpServer;

    @Override
    public void before()
    {
        httpServer = new JettyHttpServer( new AddSuffixJettyHandler() );
    }

    @Override
    public void after()
    {
        httpServer.shutdown();
    }

    public String getUrl()
    {
        return "http://127.0.0.1:" + getPort();
    }

    public Integer getPort()
    {
        return httpServer.getPort();
    }
}
