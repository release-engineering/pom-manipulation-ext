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
package org.commonjava.maven.ext.manip.io;

import org.apache.maven.settings.*;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * @author vdedik@redhat.com
 */
@Component( role = SettingsIO.class )
public class SettingsIO
{
    private static final Logger LOGGER = LoggerFactory.getLogger( SettingsIO.class );

    @Requirement
    private SettingsBuilder settingsBuilder;

    public void write( Settings settings, File settingsFile )
        throws ManipulationException
    {
        try
        {
            PrintWriter printWriter = new PrintWriter( settingsFile, "UTF-8" );
            new SettingsXpp3Writer().write( printWriter, settings );
        }
        catch ( IOException e )
        {
            throw new ManipulationException( "Failed to create repo removal backup settings.xml file.",
                                             e, settingsFile, e.getMessage() );
        }
    }

    public void update( Settings settings, File settingsFile )
        throws ManipulationException
    {
        try
        {
            Settings defaultSettings = new Settings();

            if ( settingsFile.exists() )
            {
                DefaultSettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
                settingsRequest.setGlobalSettingsFile( settingsFile );
                defaultSettings = settingsBuilder.build( settingsRequest ).getEffectiveSettings();
            }

            for ( Profile profile : settings.getProfiles() )
            {
                Iterator<Profile> i = defaultSettings.getProfiles().iterator();
                while (i.hasNext())
                {
                    if (i.next().getId().equals( profile.getId() ))
                    {
                        i.remove();
                    }
                }
                defaultSettings.addProfile( profile );
            }
            for ( String activeProfile : settings.getActiveProfiles() )
            {
                Iterator<String> i = defaultSettings.getActiveProfiles().iterator();
                while (i.hasNext())
                {
                    if (i.next().equals( activeProfile ))
                    {
                        i.remove();
                    }
                }
                defaultSettings.addActiveProfile( activeProfile );
            }
            for ( Mirror mirror : settings.getMirrors() )
            {
                defaultSettings.addMirror( mirror );
            }
            for ( Proxy proxy : settings.getProxies() )
            {
                defaultSettings.addProxy( proxy );
            }
            for ( Server server : settings.getServers() )
            {
                defaultSettings.addServer( server );
            }
            for ( String pluginGroup : settings.getPluginGroups() )
            {
                defaultSettings.addPluginGroup( pluginGroup );
            }
            if ( settings.getLocalRepository() != null )
            {
                defaultSettings.setLocalRepository( settings.getLocalRepository() );
            }

            write( defaultSettings, settingsFile );
        }
        catch ( SettingsBuildingException e )
        {
            throw new ManipulationException( "Failed to build existing settings.xml for repo removal backup.",
                                             e, settingsFile, e.getMessage() );
        }
    }
}
