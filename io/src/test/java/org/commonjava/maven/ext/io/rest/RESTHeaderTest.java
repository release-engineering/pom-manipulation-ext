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

import org.commonjava.maven.ext.io.rest.rule.MockServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;

public class RESTHeaderTest
{
    @Rule
    public final MockServer mockServer = new MockServer( new AbstractHandler()
    {
        @Override
        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response ) throws IOException
        {
            assertEquals( request.getHeader( "Foo" ),  is( "bar" ) );
            assertEquals( request.getHeader( "Bar" ),  is( "baz" ) );
        }
    } );

    @Test
    public void testVerifyHeader() throws RestException
    {
        Map<String, String> headers = new LinkedHashMap<>( 2 );
        headers.put( "Foo", "bar" );
        headers.put( "Bar", "baz" );
        DefaultTranslator translator = new DefaultTranslator( mockServer.getUrl(), 0,
                Translator.CHUNK_SPLIT_COUNT, null, "", headers );
    }
}
