package org.commonjava.maven.ext.manip.state;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class EnforcingModeTest
{

    @Test
    public void getModeReturnsOffForFalse()
    {
        assertThat( EnforcingMode.getMode( "false" ), equalTo( EnforcingMode.off ) );
    }

    @Test
    public void getModeReturnsOnForTrue()
    {
        assertThat( EnforcingMode.getMode( "true" ), equalTo( EnforcingMode.on ) );
    }

}
