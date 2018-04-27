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

package org.commonjava.maven.ext.core.state;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

public class SuffixStateTest
{
    @Test
    public void defaultSuffixIsSet()
    {
        Properties p = new Properties();
        p.setProperty( SuffixState.SUFFIX_STRIP_PROPERTY, "" );
        SuffixState s = new SuffixState( p );

        assertTrue( StringUtils.isNotEmpty( s.getSuffixStrip() ) );
    }

    @Test
    public void testDefaultSuffix()
    {
        Properties props = new Properties();
        props.setProperty( SuffixState.SUFFIX_STRIP_PROPERTY, "" );
        SuffixState s = new SuffixState( props );

        Pattern p = Pattern.compile( s.getSuffixStrip() );

        Matcher m = p.matcher( "1.0.jbossorg-101909" );
        assertTrue( m.matches() );
        assertTrue( m.group( 1 ).equals( "1.0" ) );
        assertTrue( m.group( 2 ).equals( ".jbossorg-101909" ) );

        m = p.matcher( "1.0.Final-jbossorg-1" );
        assertTrue( m.matches() );
        assertTrue( m.group( 1 ).equals( "1.0.Final" ) );
        assertTrue( m.group( 2 ).equals( "-jbossorg-1" ) );
    }

    @Test
    public void testCustomSuffix()
    {
        Properties props = new Properties();
        props.setProperty( SuffixState.SUFFIX_STRIP_PROPERTY, "(.*)(.MYSUFFIX)$" );
        SuffixState s = new SuffixState( props );

        Pattern p = Pattern.compile( s.getSuffixStrip() );

        Matcher m = p.matcher( "1.0.jbossorg-101909" );
        assertTrue( ! m.matches() );

        m = p.matcher( "1.0.MYSUFFIX" );
        assertTrue( m.matches() );
        assertTrue( m.group( 1 ).equals( "1.0" ) );
        assertTrue( m.group( 2 ).equals( ".MYSUFFIX" ) );
    }
}