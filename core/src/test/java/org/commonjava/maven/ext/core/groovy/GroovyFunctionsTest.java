/*
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

package org.commonjava.maven.ext.core.groovy;

import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.fixture.TestUtils;
import org.commonjava.maven.ext.io.PomIO;
import org.commonjava.maven.ext.io.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.io.rest.rule.MockServer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class GroovyFunctionsTest
{
    private static final String RESOURCE_BASE = "properties/";

    private AddSuffixJettyHandler handler = new AddSuffixJettyHandler();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Rule
    public MockServer mockServer = new MockServer( handler );

    @Test (expected = ManipulationException.class )
    public void testOverride() throws Exception
    {
        // Locate the PME project pom file. Use that to verify inheritance tracking.
        final File projectroot = new File ( TestUtils.resolveFileResource( RESOURCE_BASE, "" )
                                                     .getParentFile()
                                                     .getParentFile()
                                                     .getParentFile()
                                                     .getParentFile(), "pom.xml" );
        PomIO pomIO = new PomIO();
        List<Project> projects = pomIO.parseProject( projectroot );

        BaseScriptImplTest impl = new BaseScriptImplTest();
        Properties p = new Properties(  );
        p.setProperty( "restURL", mockServer.getUrl() );

        impl.setValues( null, TestUtils.createSession( p ), projects, projects.get( 0 ), InvocationStage.FIRST );

        impl.overrideProjectVersion( SimpleProjectVersionRef.parse( "org.foo:bar:1.0" ) );
    }

    private static class BaseScriptImplTest extends BaseScript
    {
        @Override
        public Object run()
        {
            return null;
        }
    }
}