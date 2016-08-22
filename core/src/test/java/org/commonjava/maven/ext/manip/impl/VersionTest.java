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
package org.commonjava.maven.ext.manip.impl;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.commonjava.maven.ext.manip.impl.Version.findHighestMatchingBuildNumber;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class VersionTest
{

    @Test
    public void testParseVersion()
        throws Exception
    {
        Version version = new Version("1.2.beta2");
        assertThat( version.getMajorVersion(), equalTo( "1" ) );
        assertThat( version.getMinorVersion(), equalTo( "2" ) );
        assertThat( version.getMicroVersion(), equalTo( "0" ) );
        assertThat( version.getQualifier(), equalTo( "beta2" ) );

        version = new Version("1");
        assertThat( version.getMajorVersion(), equalTo( "1" ) );
        assertThat( version.getMinorVersion(), equalTo( "0" ) );
        assertThat( version.getMicroVersion(), equalTo( "0" ) );
        assertThat( version.getQualifier(), equalTo( "" ) );

        version = new Version("1beta1");
        assertThat( version.getMajorVersion(), equalTo( "1" ) );
        assertThat( version.getMinorVersion(), equalTo( "0" ) );
        assertThat( version.getMicroVersion(), equalTo( "0" ) );
        assertThat( version.getQualifier(), equalTo( "beta1" ) );

        version = new Version("1-0-3.4-beta2");
        assertThat( version.getMajorVersion(), equalTo( "1" ) );
        assertThat( version.getMinorVersion(), equalTo( "0" ) );
        assertThat( version.getMicroVersion(), equalTo( "3" ) );
        assertThat( version.getQualifier(), equalTo( "4-beta2" ) );

        version = new Version("11-01-beta2_SNAPSHOT_HELLO-1");
        assertThat( version.getMajorVersion(), equalTo( "11" ) );
        assertThat( version.getMinorVersion(), equalTo( "01" ) );
        assertThat( version.getMicroVersion(), equalTo( "0" ) );
        assertThat( version.getQualifier(), equalTo( "beta2_SNAPSHOT_HELLO-1" ) );

        version = new Version("1.2.3.4.Final-X");
        assertThat( version.getMajorVersion(), equalTo( "1" ) );
        assertThat( version.getMinorVersion(), equalTo( "2" ) );
        assertThat( version.getMicroVersion(), equalTo( "3" ) );
        assertThat( version.getQualifier(), equalTo( "4.Final-X" ) );
    }

    @Test
    public void testGetOsgiVersion()
        throws Exception
    {
        Version version = new Version("1.2.beta2");
        assertThat( version.isValidOSGi(), equalTo( false ) );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.beta2" ) );

        version = new Version("1.2");
        assertThat( version.isValidOSGi(), equalTo( true ) );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2" ) );

        version = new Version("1");
        assertThat( version.isValidOSGi(), equalTo( true ) );
        assertThat( version.getOSGiVersionString(), equalTo( "1" ) );

        version = new Version("2.6");
        assertThat( version.isValidOSGi(), equalTo( true ) );
        assertThat( version.getOSGiVersionString(), equalTo( "2.6" ) );

        version = new Version("2.6");
        version.appendQualifierSuffix( "rebuild-1" );
        assertThat( version.isValidOSGi(), equalTo( true ) );
        assertThat( version.getOSGiVersionString(), equalTo( "2.6.0.rebuild-1" ) );

        version = new Version("12.1.100");
        assertThat( version.isValidOSGi(), equalTo( true ) );
        assertThat( version.getOSGiVersionString(), equalTo( "12.1.100" ) );

        version = new Version("1beta1");
        assertThat( version.isValidOSGi(), equalTo( false ) );
        assertThat( version.getOSGiVersionString(), equalTo( "1.0.0.beta1" ) );

        version = new Version("1-0-3.4-beta2.1");
        assertThat( version.isValidOSGi(), equalTo( false ) );
        assertThat( version.getOSGiVersionString(), equalTo( "1.0.3.4-beta2-1" ) );

        version = new Version("10.0.54.beta-2_3");
        assertThat( version.isValidOSGi(), equalTo( true ) );
        assertThat( version.getOSGiVersionString(), equalTo( "10.0.54.beta-2_3" ) );

    }

    @Test
    public void testGetBuildNumber()
        throws Exception
    {
        Version version = new Version("1.2");
        assertThat( version.getBuildNumber(), equalTo( null ) );

        version = new Version("1.2beta11");
        assertThat( version.getBuildNumber(), equalTo( "11" ) );

        version = new Version("1.2.0.jboss-9");
        assertThat( version.getBuildNumber(), equalTo( "9" ) );

        version = new Version("1.2.3.5");
        assertThat( version.getBuildNumber(), equalTo( "5" ) );

        version = new Version("1.2-SNAPSHOT");
        assertThat( version.getBuildNumber(), equalTo( null ) );

        version = new Version("1.2-jboss");
        assertThat( version.getBuildNumber(), equalTo( null ) );
    }

    @Test
    public void testSetQualifierSuffix_getVersion()
        throws Exception
    {
        Version version = new Version("1.2");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getVersionString(), equalTo( "1.2.jboss" ) );

        version = new Version("1.2beta11");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getVersionString(), equalTo( "1.2.beta11-jboss" ) );

        version = new Version("1.2.GA");
        version.appendQualifierSuffix( "foo" );
        assertThat( version.getVersionString(), equalTo( "1.2.GA-foo" ) );

        version = new Version("foo-bar-bad");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getVersionString(), equalTo( "foo-bar-bad-jboss" ) );

        version = new Version("1.2");
        version.appendQualifierSuffix( "-SNAPSHOT" );
        assertThat( version.getVersionString(), equalTo( "1.2.SNAPSHOT" ) );

        version = new Version("1.2");
        version.appendQualifierSuffix( "jboss-1-SNAPSHOT" );
        assertThat( version.getVersionString(), equalTo( "1.2.jboss-1-SNAPSHOT" ) );
}
    
    @Test
    public void testSetQualifierSuffix_getOSGiVersion()
        throws Exception
    {

        Version version = new Version("jboss-1-GA");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getVersionString(), equalTo( "jboss-1-GA-jboss" ) );

        version = new Version("1.2");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss" ) );

        version = new Version("1.2beta11");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.beta11-jboss" ) );

        version = new Version("1.2.0.jboss-9");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-9" ) );

        version = new Version("1.2.3.5");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.3.5-jboss" ) );

        version = new Version("1.2-SNAPSHOT");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-SNAPSHOT" ) );

        version = new Version("1.2-jboss-9-foo");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-9-foo-jboss" ) );

        version = new Version("1.2.1.Final-jboss-8");
        version.appendQualifierSuffix( "jboss" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.1.Final-jboss-8" ) );

        version = new Version("1.2.1.Final");
        version.appendQualifierSuffix( "jboss-1" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.1.Final-jboss-1" ) );

        version = new Version("1.2-GA");
        version.appendQualifierSuffix( "jboss-2" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.GA-jboss-2" ) );

        version = new Version("1.2-jboss");
        version.appendQualifierSuffix( "jboss-2" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-2" ) );

        version = new Version("1.2-jboss");
        version.appendQualifierSuffix( "jboss-2" );
        version.setBuildNumber( "3");
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-3" ) );

        version = new Version("1.2.0-SNAPSHOT");
        version.appendQualifierSuffix( "jboss-2" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-2-SNAPSHOT" ) );

        version = new Version("1.2.3.4.GA.1");
        version.appendQualifierSuffix( "jboss-1" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.3.4-GA-1-jboss-1" ) );
    }

    @Test
    public void testSetBuildNumber()
        throws Exception
    {
        Version version = new Version("1.2");
        version.setBuildNumber( "1" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.1" ) );

        version = new Version("1.2beta11");
        version.setBuildNumber( "12" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.beta-12" ) );

        version = new Version("1.2.3.5");
        version.setBuildNumber( "8" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.3.8" ) );

        version = new Version("1.2-SNAPSHOT");
        version.setBuildNumber( "1" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.1-SNAPSHOT" ) );

        version = new Version("1.2-jboss-9-foo");
        version.setBuildNumber( "10" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-9-foo-10" ) );

        version = new Version("1.2.1.Final-jboss-8");
        version.setBuildNumber( "9" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.1.Final-jboss-9" ) );

        version = new Version("1.2.0-GA");
        version.appendQualifierSuffix( "foo" );
        version.setBuildNumber( "2" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.GA-foo-2" ) );
    }

    @Test
    public void testFindHighestMatchingBuildNumber()
    {
        final Set<String> versionSet = new HashSet<>();
        Version version = new Version("1.2.0.Final-foo");
        versionSet.add( "1.2.0.Final-foo-1" );
        versionSet.add( "1.2.0.Final-foo-2" );
        assertThat( findHighestMatchingBuildNumber( version, versionSet ), equalTo( 2 ) );
        versionSet.clear();

        version = new Version("0.0.4");
        version.appendQualifierSuffix( "redhat-0" );
        versionSet.add( "0.0.1" );
        versionSet.add( "0.0.2" );
        versionSet.add( "0.0.3" );
        versionSet.add( "0.0.4" );
        versionSet.add( "0.0.4.redhat-2" );
        assertThat( findHighestMatchingBuildNumber( version, versionSet ), equalTo( 2 ) );
        versionSet.clear();

        version = new Version("1.2-foo-1");
        versionSet.add( "1.2-foo-4" );
        assertThat( findHighestMatchingBuildNumber( version, versionSet ), equalTo( 4 ) );
        versionSet.clear();
    }

    @Test
    public void testZeroFill_FindHighestMatchingBuildNumber()
    {
        final Set<String> versionSet = new HashSet<>();
        final Version majorOnlyVersion = new Version( "7" );
        majorOnlyVersion.appendQualifierSuffix( "redhat" );
        System.out.println("OSGi version: " + majorOnlyVersion.getOSGiVersionString());
        versionSet.add( "7.0.0.redhat-2" );
        assertThat( findHighestMatchingBuildNumber( majorOnlyVersion, versionSet ), equalTo( 2 ) );
        versionSet.clear();

        final Version majorMinorVersion = new Version( "7.1" );
        majorMinorVersion.appendQualifierSuffix( "redhat" );
        System.out.println("OSGi version: " + majorMinorVersion.getOSGiVersionString());
        versionSet.add( "7.1.0.redhat-2" );
        assertThat( findHighestMatchingBuildNumber( majorMinorVersion, versionSet ), equalTo( 2 ) );
        versionSet.clear();
    }
    
    @Test
    public void testSetQualifierSuffix_MulitpleTimes()
        throws Exception
    {

        Version version = new Version("1.2.0");
        version.appendQualifierSuffix( "jboss-1" );
        version.appendQualifierSuffix( "foo" );
        assertThat( version.getVersionString(), equalTo( "1.2.0.jboss-1-foo" ) );

        version = new Version("1.2");
        version.appendQualifierSuffix( "jboss-1" );
        version.appendQualifierSuffix( "jboss-2" );
        assertThat( version.getOSGiVersionString(), equalTo( "1.2.0.jboss-2" ) );

    }
}
