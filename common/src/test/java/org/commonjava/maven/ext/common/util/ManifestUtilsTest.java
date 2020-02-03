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

package org.commonjava.maven.ext.common.util;

import org.commonjava.maven.ext.common.ManipulationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ManifestUtilsTest
{
    @Rule
    public final SystemOutRule systemRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

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
        ManifestUtils.getManifestInformation( ManifestUtilsTest.class );

        assertTrue( systemRule.getLog()
                              .contains( "Unable to retrieve manifest for class "
                                                         + "org.commonjava.maven.ext.common.util.ManifestUtilsTest as "
                                                         + "location is a directory not a jar" ) );
    }

    @Test
    public void testJarClass() throws ManipulationException
    {
        String m = ManifestUtils.getManifestInformation( Test.class );
        assertTrue( m.contains( "4" ) );
    }
}