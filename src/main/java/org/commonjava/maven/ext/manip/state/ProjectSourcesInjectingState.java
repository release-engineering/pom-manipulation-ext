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

    public static final String DEFAULT_BMMP_VERSION = "1.3.5-RC1";

    private final boolean enabled;

    private final boolean metadataEnabled;

    private final String projectSrcPluginVersion;

    private final String bmmpVersion;

    /**
     * Detects the project.src.skip and project.src.version user properties. Sets the enabled flag and the plugin version accordingly.
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
     */
    public boolean isBuildMetadataPluginEnabled()
    {
        return metadataEnabled;
    }

    /**
     * @see #PROJECT_SOURCES_PLUGIN_VERSION_PROPERTY
     * @see #DEFAULT_PROJECT_SOURCES_PLUGIN_VERSION
     */
    public String getProjectSourcesPluginVersion()
    {
        return projectSrcPluginVersion;
    }

    /**
     * @see #BMMP_VERSION_PROPERTY
     * @see #DEFAULT_BMMP_VERSION
     */
    public String getBuildMetadataPluginVersion()
    {
        return bmmpVersion;
    }

}
