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
package org.commonjava.maven.ext.common.util;

import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.github.valfirst.slf4jtest.TestLoggerFactoryResetRule;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class WildcardMapTest
{
    private WildcardMap<String> map;

    TestLogger logger = TestLoggerFactory.getTestLogger( WildcardMap.class);

    @Rule
    public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

    @Before
    public void setUp()
    {
        map = new WildcardMap<>();
    }

    @Test
    public void testContainsKey()
    {
        map.put( SimpleProjectRef.parse( "org.group:new-artifact" ), "1.2");

        Assert.assertFalse( map.containsKey( SimpleProjectRef.parse( "org.group:*" )));
        Assert.assertFalse( map.containsKey( SimpleProjectRef.parse( "org.group:old-artifact" )));
    }

    @Test
    public void testGet()
    {
        final String value = "1.2";
        ProjectRef key1 = SimpleProjectRef.parse( "org.group:new-artifact" );
        ProjectRef key2 = SimpleProjectRef.parse( "org.group:new-new-artifact" );

        map.put(key1, value);
        map.put(key2, value);

        Assert.assertEquals( value, map.get( key1 ) );
        Assert.assertEquals( value, map.get( key2 ) );
    }

    @Test
    public void testGetSingle()
    {
        final String value = "1.2";

        map.put( SimpleProjectRef.parse( "org.group:new-artifact" ), value);

        Assert.assertNotEquals( value, map.get( SimpleProjectRef.parse( "org.group:i-dont-exist-artifact" ) ) );
    }

    @Test
    public void testPut()
    {
        ProjectRef key = SimpleProjectRef.parse( "foo:bar" );

        map.put(key, "value");
        Assert.assertTrue( "Should have retrieved value", map.containsKey( key));
    }

    @Test
    public void testPutWildcard()
    {
        ProjectRef key1 = SimpleProjectRef.parse( "org.group:*" );
        ProjectRef key2 = SimpleProjectRef.parse( "org.group:artifact" );
        ProjectRef key3 = SimpleProjectRef.parse( "org.group:new-artifact" );

        map.put(key1, "1.1");

        Assert.assertTrue( "Should have retrieved wildcard value", map.containsKey( key2));
        Assert.assertTrue( "Should have retrieved wildcard value", map.containsKey( key1));

        map.put(key3, "1.2");

        Assert.assertTrue( "Should have retrieved wildcard value", map.containsKey( key2));
        Assert.assertTrue( "Should have retrieved wildcard value", map.containsKey( key1));

        Assert.assertTrue( logger.getLoggingEvents().stream().anyMatch( e -> e.getMessage().contains( "Unable to add org.group:new-artifact with value 1.2 as wildcard mapping for org.group already exists")));

    }

    @Test
    public void testPutWildcardSecond()
    {
        ProjectRef key1 = SimpleProjectRef.parse( "org.group:artifact" );
        ProjectRef key2 = SimpleProjectRef.parse( "org.group:*" );

        map.put(key1, "1.1");
        map.put(key2, "1.2");

        Assert.assertTrue( "Should have retrieved explicit value via wildcard", map.containsKey( key1));
        Assert.assertTrue( "Should have retrieved wildcard value", map.containsKey( key2));
        Assert.assertNotEquals( "Should not have retrieved value 1.1", "1.1", map.get( key1 ) );

        Assert.assertTrue( logger.getLoggingEvents().stream().anyMatch( e -> e.getMessage().contains( "Emptying map with keys [artifact] as replacing with wildcard mapping org.group:*")));
    }
}
