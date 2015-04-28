package org.commonjava.maven.ext.manip.fixture;

import org.apache.maven.settings.building.SettingsBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( PlexusTestRunner.class )
@Component( role = TestWithPlexusRunner.class )
public class TestWithPlexusRunner
{

    @Requirement
    private SettingsBuilder settingsBuilder;

    @Test
    public void bootUp()
    {
        System.out.println( "Settings builder: " + settingsBuilder );
    }

}
