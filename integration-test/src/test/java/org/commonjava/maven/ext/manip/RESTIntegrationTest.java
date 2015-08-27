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

package org.commonjava.maven.ext.manip;

import org.commonjava.maven.ext.manip.rest.MockVersionTranslator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.commonjava.maven.ext.manip.CliTestUtils.getDefaultTestLocation;
import static org.commonjava.maven.ext.manip.CliTestUtils.runLikeInvoker;

public class RESTIntegrationTest
{
    private static MockVersionTranslator versionTranslator;

    @BeforeClass
    public static void startUp() {
        versionTranslator = new MockVersionTranslator();
    }

    @AfterClass
    public static void tearDown() {
        versionTranslator.shutdownMockServer();
    }

    @Test
    public void testIntegration() throws Exception
    {
        String test = getDefaultTestLocation( "rest-dependency-version-manip-child-module" );
        runLikeInvoker( test );
    }
}
