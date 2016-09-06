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
package org.commonjava.maven.ext.manip.state;

import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VersioningStateTest
{

    @Test
    public void disabledByDefault()
    {
        final VersioningState state = new VersioningState( new Properties() );

        assertThat( state.isEnabled(), equalTo( false ) );
    }

    @Test
    public void enableViaStaticSuffix()
    {
        final Properties p = new Properties();
        p.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, "rebuild-1" );

        final VersioningState state = new VersioningState( p );

        assertThat( state.isEnabled(), equalTo( true ) );
    }

    @Test
    public void enableViaIncrementalSuffix()
    {
        final Properties p = new Properties();
        p.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "rebuild-1" );

        final VersioningState state = new VersioningState( p );

        assertThat( state.isEnabled(), equalTo( true ) );
    }

}
