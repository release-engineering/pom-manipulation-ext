/*
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.commonjava.maven.ext.io.rest;

import kong.unirest.Unirest;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.io.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.io.rest.rule.MockServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author vdedik@redhat.com
 */
public class BlacklistTranslatorTest
{
    private DefaultTranslator blacklistTranslator;

    @Rule
    public TestName testName = new TestName();

    private AddSuffixJettyHandler blacklist = new AddSuffixJettyHandler( "/listings/blacklist/ga", AddSuffixJettyHandler.DEFAULT_SUFFIX );

    @Rule
    public MockServer mockServer = new MockServer( blacklist );

    @Before
    public void before()
    {
        LoggerFactory.getLogger( BlacklistTranslatorTest.class ).info( "Executing test " + testName.getMethodName() );

        this.blacklistTranslator = new DefaultTranslator( mockServer.getUrl(), 0, Translator.CHUNK_SPLIT_COUNT, "",
                                                          "" );
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
    public void testFindBlacklisted() throws RestException
    {
        SimpleProjectRef ga = new SimpleProjectRef( "com.example", "example" );

        List<ProjectVersionRef> actualResult = blacklistTranslator.findBlacklisted( ga );

        assertEquals( 1, actualResult.size() );
        assertTrue( actualResult.get( 0 ).getVersionString().contains( AddSuffixJettyHandler.DEFAULT_SUFFIX ));
    }
}
