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

import org.apache.maven.model.Model;
import com.github.valfirst.slf4jtest.TestLogger;
import com.github.valfirst.slf4jtest.TestLoggerFactory;
import com.github.valfirst.slf4jtest.TestLoggerFactoryResetRule;
import org.commonjava.maven.ext.common.ManipulationException;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ManifestUtilsTest
{
    @Rule
    public TestLoggerFactoryResetRule testLoggerFactoryResetRule = new TestLoggerFactoryResetRule();

    @Test
    public void testNoClass()
    {
        try
        {
            ManifestUtils.getManifestInformation( null );
            fail("No exception thrown");
        }
        catch (ManipulationException e)
        {
            assertTrue( e.getMessage().contains( "No target specified" ) );
        }
    }

    @Test
    public void testThisClass() throws ManipulationException
    {
        TestLogger logger = TestLoggerFactory.getTestLogger( ManifestUtils.class);

        ManifestUtils.getManifestInformation( ManifestUtilsTest.class );

        assertTrue( logger.getLoggingEvents().stream().anyMatch( e -> e.getFormattedMessage().contains( "Unable to retrieve manifest for class "
                                                         + "org.commonjava.maven.ext.common.util.ManifestUtilsTest as "
                                                         + "location is a directory not a jar" ) ) );
    }

    @Test
    public void testThirdPartyClass() throws ManipulationException
    {
        String result = ManifestUtils.getManifestInformation( Model.class );

        assertTrue( result.contains( "3.5.0 ( SHA: null )" ) );
    }

    @Test
    public void testJarClass() throws ManipulationException
    {
        String m = ManifestUtils.getManifestInformation( Test.class );
        assertTrue( m.contains( "4" ) );
    }
}
