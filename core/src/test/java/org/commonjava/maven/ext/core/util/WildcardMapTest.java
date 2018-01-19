/**
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
package org.commonjava.maven.ext.core.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WildcardMapTest
{
    private WildcardMap<String> map;
    private ListAppender<ILoggingEvent> m_listAppender;

    @Before
    public void setUp() throws Exception
    {
        m_listAppender = new ListAppender<>();
        m_listAppender.start();

        Logger root = (Logger) LoggerFactory.getLogger(WildcardMap.class);
        root.addAppender(m_listAppender);

        map = new WildcardMap<>();
    }

    @After
    public void tearDown() throws Exception
    {
        map = null;

        Logger root = (Logger) LoggerFactory.getLogger(WildcardMap.class);
        root.detachAppender(m_listAppender);
    }

    @Test
    public void testContainsKey() throws Exception
    {
        map.put( SimpleProjectRef.parse( "org.group:new-artifact" ), "1.2");

        assertFalse(map.containsKey( SimpleProjectRef.parse( "org.group:*" )));
        assertFalse(map.containsKey( SimpleProjectRef.parse( "org.group:old-artifact" )));
    }

    @Test
    public void testGet() throws Exception
    {
        final String value = "1.2";
        ProjectRef key1 = SimpleProjectRef.parse( "org.group:new-artifact" );
        ProjectRef key2 = SimpleProjectRef.parse( "org.group:new-new-artifact" );

        map.put(key1, value);
        map.put(key2, value);

        assertTrue(value.equals(map.get(key1)));
        assertTrue(value.equals(map.get(key2)));
    }

    @Test
    public void testGetSingle() throws Exception
    {
        final String value = "1.2";

        map.put( SimpleProjectRef.parse( "org.group:new-artifact" ), value);

        assertFalse(value.equals(map.get( SimpleProjectRef.parse( "org.group:i-dont-exist-artifact" ))));
    }

    @Test
    public void testPut() throws Exception
    {
        ProjectRef key = SimpleProjectRef.parse( "foo:bar" );

        map.put(key, "value");
        assertTrue("Should have retrieved value", map.containsKey(key));
    }

    @Test
    public void testPutWildcard() throws Exception
    {
        ProjectRef key1 = SimpleProjectRef.parse( "org.group:*" );
        ProjectRef key2 = SimpleProjectRef.parse( "org.group:artifact" );
        ProjectRef key3 = SimpleProjectRef.parse( "org.group:new-artifact" );

        map.put(key1, "1.1");

        assertTrue("Should have retrieved wildcard value", map.containsKey(key2));
        assertTrue("Should have retrieved wildcard value", map.containsKey(key1));

        map.put(key3, "1.2");

        assertTrue("Should have retrieved wildcard value", map.containsKey(key2));
        assertTrue("Should have retrieved wildcard value", map.containsKey(key1));

        assertThat(m_listAppender.list.toString(),
                containsString("Unable to add org.group:new-artifact with value 1.2 as wildcard mapping for org.group already exists"));

    }

    @Test
    public void testPutWildcardSecond() throws Exception
    {
        ProjectRef key1 = SimpleProjectRef.parse( "org.group:artifact" );
        ProjectRef key2 = SimpleProjectRef.parse( "org.group:*" );

        map.put(key1, "1.1");
        map.put(key2, "1.2");

        assertTrue("Should have retrieved explicit value via wildcard", map.containsKey(key1));
        assertTrue("Should have retrieved wildcard value", map.containsKey(key2));
        assertFalse("Should not have retrieved value 1.1", map.get(key1).equals("1.1"));

        assertThat(m_listAppender.list.toString(),
                containsString("Emptying map with keys [artifact] as replacing with wildcard mapping org.group:*"));

    }
}