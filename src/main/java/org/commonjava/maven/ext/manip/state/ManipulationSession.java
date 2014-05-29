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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Mirror;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.impl.Manipulator;
import org.commonjava.maven.ext.manip.model.Project;

/**
 * Repository for components that help manipulate POMs as needed, and state related to each {@link Manipulator}
 * (which contains configuration and changes to be applied). This is basically a clearing house for state required by the different parts of the
 * manipulator extension.
 *
 * @author jdcasey
 */
@Component( role = ManipulationSession.class, instantiationStrategy = "singleton" )
public class ManipulationSession
{

    public static final String MANIPULATIONS_DISABLED_PROP = "manipulation.disable";

    @Requirement( role = Manipulator.class )
    private Map<String, Manipulator> manipulators;

    @Requirement
    private Logger logger;

    private final Map<Class<?>, State> states = new HashMap<Class<?>, State>();

    private MavenSession mavenSession;

    public ManipulationSession()
    {
        System.out.println( "[INFO] Maven-Manipulation-Extension " + getClass().getPackage()
                                                                               .getImplementationVersion() );
    }

    /**
     * True (enabled) by default, this is the master kill switch for all manipulations. Manipulator implementations MAY also be enabled/disabled
     * individually.
     *
     * @see #MANIPULATIONS_DISABLED_PROP
     * @see VersioningState#isEnabled()
     */
    public boolean isEnabled()
    {
        return !Boolean.valueOf( getUserProperties().getProperty( MANIPULATIONS_DISABLED_PROP, "false" ) );
    }

    public MavenExecutionRequest getRequest()
    {
        return mavenSession == null ? null : mavenSession.getRequest();
    }

    public void setState( final State state )
    {
        states.put( state.getClass(), state );
    }

    public <T extends State> T getState( final Class<T> stateType )
    {
        return stateType.cast( states.get( stateType ) );
    }

    public ProjectBuildingRequest getProjectBuildingRequest()
    {
        return mavenSession == null ? null : mavenSession.getRequest()
                                                         .getProjectBuildingRequest();
    }

    public boolean isRecursive()
    {
        return mavenSession == null ? false : mavenSession.getRequest()
                                                          .isRecursive();
    }

    public void setMavenSession( final MavenSession mavenSession )
    {
        this.mavenSession = mavenSession;
    }

    /**
     * Map of Group:Artifact to Model
     */
    private Map<String, Model> rawModels;

    /**
     * List of <code>Project</code> instances.
     */
    private List<Project> projects;

    public void setManipulatedModels( final Map<String, Model> rawModels )
    {
        this.rawModels = rawModels;
    }

    public Map<String, Model> getManipulatedModels()
    {
        return rawModels;
    }

    public Properties getUserProperties()
    {
        return mavenSession == null ? new Properties() : mavenSession.getRequest()
                                                                     .getUserProperties();
    }

    public void setProjects( final List<Project> projects )
    {
        this.projects = projects;
    }

    public List<Project> getProjects()
    {
        return projects;
    }

    public List<ArtifactRepository> getRemoteRepositories()
    {
        return mavenSession == null ? null : mavenSession.getRequest()
                                                         .getRemoteRepositories();
    }

    public File getTargetDir()
    {
        if ( mavenSession == null )
        {
            return new File( "target" );
        }

        final File pom = mavenSession.getRequest()
                                     .getPom();
        if ( pom == null )
        {
            return new File( "target" );
        }

        return new File( pom.getParentFile(), "target" );
    }

    public ArtifactRepository getLocalRepository()
    {
        return mavenSession == null ? null : mavenSession.getRequest()
                                                         .getLocalRepository();
    }

    public List<Mirror> getMirrors()
    {
        return mavenSession == null || mavenSession.getSettings() == null ? Collections.<Mirror> emptyList()
                        : mavenSession.getSettings()
                                      .getMirrors();
    }

}
