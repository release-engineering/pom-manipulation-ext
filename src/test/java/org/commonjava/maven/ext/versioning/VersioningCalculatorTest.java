package org.commonjava.maven.ext.versioning;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Properties;

import org.junit.Test;

public class VersioningCalculatorTest
{

    public void initFailsWithoutSuffixProperty()
    {
        final Properties props = new Properties();

        final VersionCalculator modder = new VersionCalculator( props );

        assertThat( modder.isEnabled(), equalTo( false ) );
    }

    @Test
    public void indempotency()
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2";

        String result = modder.calculate( v );
        assertThat( result, equalTo( v + "." + s ) );

        result = modder.calculate( result );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applyNonSerialSuffix_NumericVersionTail()
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2";

        final String result = modder.calculate( v );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applyNonSerialSuffix_NonNumericVersionTail()
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2.GA";

        final String result = modder.calculate( v );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySerialSuffix_NumericVersionTail()
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2";

        final String result = modder.calculate( v );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applySerialSuffix_NonNumericVersionTail()
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2.GA";

        final String result = modder.calculate( v );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySerialSuffix_NumericVersionTail_OverwriteExisting()
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2";
        final String os = ".foo-1";

        final String result = modder.calculate( v + os );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applySerialSuffix_NonNumericVersionTail_OverwriteExisting()
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2.GA";
        final String os = "-foo-1";

        final String result = modder.calculate( v + os );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySuffixBeforeSNAPSHOT()
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2.GA";
        final String sn = "-SNAPSHOT";

        final String result = modder.calculate( v + sn );
        assertThat( result, equalTo( v + "-" + s + sn ) );
    }

    @Test
    public void applySuffixBeforeSNAPSHOT_OverwriteExisting()
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, s );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2.GA";
        final String sn = "-SNAPSHOT";
        final String os = "-foo-1";

        final String result = modder.calculate( v + os + sn );
        assertThat( result, equalTo( v + "-" + s + sn ) );
    }

    @Test
    public void incrementExistingSerialSuffix()
    {
        final Properties props = new Properties();

        props.setProperty( VersionCalculator.VERSION_SUFFIX_SYSPROP, "foo-0" );
        props.setProperty( VersionCalculator.INCREMENT_SERIAL_SYSPROP, Boolean.toString( Boolean.TRUE ) );

        final VersionCalculator modder = new VersionCalculator( props );

        final String v = "1.2.GA";
        final String os = "-foo-1";
        final String ns = "foo-2";

        final String result = modder.calculate( v + os );
        assertThat( result, equalTo( v + "-" + ns ) );
    }

}
