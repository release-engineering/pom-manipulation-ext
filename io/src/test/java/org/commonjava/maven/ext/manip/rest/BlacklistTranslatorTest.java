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
package org.commonjava.maven.ext.manip.rest;

import com.mashape.unirest.http.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.manip.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.manip.rest.rule.MockServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.commonjava.maven.ext.manip.rest.Translator.RestProtocol;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author vdedik@redhat.com
 */
@RunWith( Parameterized.class)
public class BlacklistTranslatorTest
{
    private DefaultTranslator blacklistTranslator;

    private RestProtocol protocol;

    @Parameterized.Parameters()
    public static Collection<Object[]> data()
    {
        return Arrays.asList( new Object[][] { { RestProtocol.CURRENT } } );
    }

    @Rule
    public TestName testName = new TestName();

    private AddSuffixJettyHandler blacklist = new AddSuffixJettyHandler( "/listings/blacklist/ga", AddSuffixJettyHandler.DEFAULT_SUFFIX );

    @Rule
    public MockServer mockServer = new MockServer( blacklist );

    @Before
    public void before()
    {
        LoggerFactory.getLogger( DefaultTranslator.class ).info( "Executing test " + testName.getMethodName() );

        this.blacklistTranslator = new DefaultTranslator( mockServer.getUrl(), protocol, 0, Translator.CHUNK_SPLIT_COUNT );
    }

    public BlacklistTranslatorTest( RestProtocol protocol)
    {
        this.protocol = protocol;
    }

    @Test
    public void testConnection()
    {
        try
        {
            Unirest.post( mockServer.getUrl() ).asString();
        }
        catch ( Exception e )
        {
            fail( "Failed to connect to server, exception message: " + e.getMessage() );
        }
    }

    @Test
    public void testFindBlacklisted()
    {
        SimpleProjectRef ga = new SimpleProjectRef( "com.example", "example" );

        List<ProjectVersionRef> actualResult = blacklistTranslator.findBlacklisted( ga );

        assertTrue( actualResult.size() == 1 );
        assertTrue( actualResult.get( 0 ).getVersionString().contains( AddSuffixJettyHandler.DEFAULT_SUFFIX ));

    }
}
