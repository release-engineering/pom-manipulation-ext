/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.ext.manip.state;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;

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
