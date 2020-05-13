/*
 * Copyright (C) 2012 Red Hat, Inc.
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

package org.commonjava.maven.ext.io;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.DefaultSettingsReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SettingsIOTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void verifySettingsEncoding() throws IOException
    {
        URL resource = SettingsIOTest.class.getResource( "settings.xml" );
        assertNotNull( resource );
        File originalSettingsFile = new File( resource.getFile() );
        assertTrue( originalSettingsFile.exists() );

        Settings settings = new DefaultSettingsReader().read( originalSettingsFile, Collections.emptyMap() );
        assertNull( settings.getModelEncoding() );

        resource = SettingsIOTest.class.getResource( "settings-wth-encoding.xml" );
        assertNotNull( resource );
        originalSettingsFile = new File( resource.getFile() );
        assertTrue( originalSettingsFile.exists() );
        settings = new DefaultSettingsReader().read( originalSettingsFile, Collections.emptyMap() );
        assertEquals( "UTF-8", settings.getModelEncoding() );
    }
}