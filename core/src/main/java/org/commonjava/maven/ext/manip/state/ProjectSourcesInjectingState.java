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
package org.commonjava.maven.ext.manip.state;

import java.util.Properties;

import org.commonjava.maven.ext.manip.impl.ProjectSourcesInjectingManipulator;

/**
 * Captures configuration parameters for use with {@link ProjectSourcesInjectingManipulator}. This state implementation captures two properties:
 *
 * <ul>
 *   <li><b>project.src.skip</b> - If true, don't try to inject the project-sources-maven-plugin.</li>
 *   <li><b>project.src.version</b> - The version of the project-sources-maven-plugin to be injected.</li>
 * </ul>
 */
public class ProjectSourcesInjectingState
    implements State
{

    /** Set this property to true using <code>-Dproject.src.skip=true</code> in order to turn off injection of the project-sources plugin. */
    public static final String PROJECT_SOURCES_SKIP_PROPERTY = "project.src.skip";

    /** Set this property to true using <code>-Dproject.src.skip=true</code> in order to turn off injection of the project-sources plugin. */
    public static final String BMMP_SKIP_PROPERTY = "project.meta.skip";

    /** Set this property to control the version of the project-sources plugin to be injected. */
    public static final String PROJECT_SOURCES_PLUGIN_VERSION_PROPERTY = "project.src.version";

    /** The default plugin version to use in case no alternative version is specified on the command line. */
    public static final String DEFAULT_PROJECT_SOURCES_PLUGIN_VERSION = "0.3";

    /** Set this property to control the version of the build-metadata plugin to be injected. */
    public static final String BMMP_VERSION_PROPERTY = "project.meta.version";

    public static final String DEFAULT_BMMP_VERSION = "1.5.2";

    private final boolean enabled;

    private final boolean metadataEnabled;

    private final String projectSrcPluginVersion;

    private final String bmmpVersion;

    static
    {
        State.activeByDefault.add( ProjectSourcesInjectingState.class );
    }

    /**
     * Detects the project.src.skip and project.src.version user properties. Sets the enabled flag and the plugin version accordingly.
     *
     * @param userProperties the properties for the manipulator
     */
    public ProjectSourcesInjectingState( final Properties userProperties )
    {
        enabled = !Boolean.parseBoolean( userProperties.getProperty( PROJECT_SOURCES_SKIP_PROPERTY, "false" ) );
        metadataEnabled = !Boolean.parseBoolean( userProperties.getProperty( BMMP_SKIP_PROPERTY, "false" ) );

        projectSrcPluginVersion =
            userProperties.getProperty( PROJECT_SOURCES_PLUGIN_VERSION_PROPERTY, DEFAULT_PROJECT_SOURCES_PLUGIN_VERSION );
        bmmpVersion = userProperties.getProperty( BMMP_VERSION_PROPERTY, DEFAULT_BMMP_VERSION );
    }

    /**
     * @see ProjectSourcesInjectingState#PROJECT_SOURCES_SKIP_PROPERTY
     */
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @see ProjectSourcesInjectingState#BMMP_SKIP_PROPERTY
     * @return whether the BuildMetadata plugin is enabled.
     */
    public boolean isBuildMetadataPluginEnabled()
    {
        return metadataEnabled;
    }

    /**
     * @see #PROJECT_SOURCES_PLUGIN_VERSION_PROPERTY
     * @see #DEFAULT_PROJECT_SOURCES_PLUGIN_VERSION
     * @return the ProjectSources plugin version
     */
    public String getProjectSourcesPluginVersion()
    {
        return projectSrcPluginVersion;
    }

    /**
     * @see #BMMP_VERSION_PROPERTY
     * @see #DEFAULT_BMMP_VERSION
     * @return the BuildMetadata plugin version
     */
    public String getBuildMetadataPluginVersion()
    {
        return bmmpVersion;
    }

}
