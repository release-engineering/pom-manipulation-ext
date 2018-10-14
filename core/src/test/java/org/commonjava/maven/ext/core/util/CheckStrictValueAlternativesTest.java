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
package org.commonjava.maven.ext.core.util;

import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.CommonState;
import org.commonjava.maven.ext.core.state.VersioningState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CheckStrictValueAlternativesTest
{
    private static final ManipulationSession session = new ManipulationSession();

    private static final Logger LOGGER = LoggerFactory.getLogger( CheckStrictValueAlternativesTest.class );

    @Before
    public void beforeTest()
    {
        Properties user = new Properties();
        user.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP.getCurrent(), "temporary-redhat" );
        final VersioningState vs = new VersioningState( user );
        session.setState( vs );
        if ( ! strictIgnoreSuffix)
        {
            user.setProperty( CommonState.STRICT_ALIGNMENT_IGNORE_SUFFIX, "false" );
        }
        final CommonState cs = new CommonState( user );
        session.setState( cs );
    }

    @Parameterized.Parameters( name = "{0} --> {1}" )
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                        // Format : Source -> Target :: Result :: AlignmentSuffix
                        { "2.6", "2.6.0.redhat-9", true, true },
                        { "2.6", "2.6.0", true, true },
                        { "1.0.0", "1.0.0", true, true },
                        { "2.6.0.Final", "2.6.0.Final-redhat-5", true, true },
                        { "2.6.Final", "2.6.0.Final-redhat-3", true, true },
                        { "2.6.Final", "2.6.1.Final-redhat-3", false, true },
                        { "1.0.jbossorg-1", "1.0.redhat-1", false, true },
                        { "1.0.redhat-4", "1.0.redhat-3", false, true },
                        { "3.2.1.redhat-4", "3.2.1.redhat-3", false, true },
                        { "3.2.0.redhat-4", "3.2.0.redhat-6", true, true },
                        { "3.2.0.redha-1", "3.2.0.redhat-6", false, true },
                        { "3.1.0.redhat-1", "3.2.0.redhat-1", false, true },
                        { "3.2.0.redhat-6", "3.2.0.redhat-4", false, true },
                        { "1.2.0.redhat-1", "3.2.0.redhat-6", false, true },
                        { "3.2.0.Final.redhat-6", "3.2.0.redhat-4", false, true },
                        { "3.2.redhat-1", "3.2.0.redhat-4", true, true },
                        { "3.2.Qualifier", "3.2.Qualifier-redhat-5", true, true },


                        // New strict checks...
                        { "2.6", "2.6.0.temporary-redhat-1", true, true },
                        { "2.6.0.temporary-redhat-2", "2.6.0.temporary-redhat-1", false, true },
                        { "2.6.0.temporary-redhat-2", "2.6.0.temporary-redhat-3", true, true },
                        { "1.0.0", "1.0.0.Final.temporary-redhat-1", false, true },
                        { "3.2.0.Final-redhat-1", "3.2.0.Final-temporary-redhat-6", true, true },
                        { "3.2.0.Final-redhat-10", "3.2.1.Final-temporary-redhat-6", false, true },
                        { "3.2.0.temporary-redhat-4", "3.2.0.temporary-redhat-5", true, true },
                        { "6.2.0.Final-temporary-redhat-2", "6.2.0.Final-redhat-1", false, true }
        });
    }

    private String source;
    private String target;
    private boolean result;
    private boolean strictIgnoreSuffix;

    public CheckStrictValueAlternativesTest( String source, String target, boolean result, boolean strictIgnoreSuffix)
    {
        this.source = source;
        this.target = target;
        this.result = result;
        this.strictIgnoreSuffix = strictIgnoreSuffix;
    }

    @Test
    public void testCheckStrictValue()
    {
        LOGGER.info ("Testing source {} to target {}", source, target);

        assertEquals( result, PropertiesUtils.checkStrictValue( session, source, target ) );
    }
}
