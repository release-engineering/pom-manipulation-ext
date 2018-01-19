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
package org.commonjava.maven.ext.core;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.session.MavenSessionHandler;
import org.commonjava.maven.ext.common.util.ManifestUtils;
import org.commonjava.maven.ext.core.impl.Manipulator;
import org.commonjava.maven.ext.core.state.State;
import org.commonjava.maven.ext.core.state.VersioningState;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * Repository for components that help manipulate POMs as needed, and state related to each {@link Manipulator}
 * (which contains configuration and changes to be applied). This is basically a clearing house for state required by the different parts of the
 * manipulator extension.
 *
 * @author jdcasey
 */
@Component( role = ManipulationSession.class, instantiationStrategy = "singleton" )
public class ManipulationSession
                implements MavenSessionHandler
{

    private static final String MANIPULATIONS_DISABLED_PROP = "manipulation.disable";

    @Requirement( role = Manipulator.class )
    private Map<String, Manipulator> manipulators;

    private final Map<Class<?>, State> states = new HashMap<>();

    private MavenSession mavenSession;

    /**
     * List of <code>Project</code> instances.
     */
    private List<Project> projects;

    private ManipulationException error;

    public  ManipulationSession()
    {
        try
        {
            System.out.println( "[INFO] Maven-Manipulation-Extension " + ManifestUtils.getManifestInformation() );
        }
        catch ( ManipulationException e )
        {
        }
    }

    /**
     * True (enabled) by default, this is the master kill switch for all manipulations. Manipulator implementations MAY also be enabled/disabled
     * individually.
     *
     * @see #MANIPULATIONS_DISABLED_PROP
     * @see VersioningState#isEnabled()
     *
     * @return whether the PME subsystem is enabled.
     */
    public boolean isEnabled()
    {
        return !Boolean.valueOf( getUserProperties().getProperty( MANIPULATIONS_DISABLED_PROP, "false" ) );
    }

    public void setState( final State state )
    {
        states.put( state.getClass(), state );
    }

    public HashSet<Entry<Class<?>, State>> getStatesCopy() {
        return new HashSet<Entry<Class<?>, State>>( states.entrySet() );
    }

    public <T extends State> T getState( final Class<T> stateType )
    {
        return stateType.cast( states.get( stateType ) );
    }

    public void setMavenSession( final MavenSession mavenSession )
    {
        this.mavenSession = mavenSession;
    }

    @Override
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

    @Override
    public List<ArtifactRepository> getRemoteRepositories()
    {
        return mavenSession == null ? null : mavenSession.getRequest()
                                                         .getRemoteRepositories();
    }


    @Override
    public File getPom() throws ManipulationException
    {
        if (mavenSession == null)
        {
            throw new ManipulationException( "Invalid session" );
        }

        return mavenSession.getRequest().getPom();
    }

    @Override
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

    @Override
    public ArtifactRepository getLocalRepository()
    {
        return mavenSession == null ? null : mavenSession.getRequest()
                                                         .getLocalRepository();
    }

    /**
     * Used by extension ManipulatingEventSpy to store any errors during project construction and manipulation
     * @param error record any exception that occurred.
     */
    public void setError( final ManipulationException error )
    {
        this.error = error;
    }

    /**
     * Used by extension ManipulatinglifeCycleParticipant to retrieve any errors stored
     * by ManipulatingEventSpy
     * @return ManipulationException
     */
    public ManipulationException getError()
    {
        return error;
    }

    @Override
    public List<String> getActiveProfiles()
    {
        return mavenSession == null || mavenSession.getRequest() == null ? null : mavenSession.getRequest()
                                                                                              .getActiveProfiles();
    }

    @Override
    public Settings getSettings()
    {
        return mavenSession == null ? null : mavenSession.getSettings();
    }


    /**
     * Checks all known states to determine whether any are enabled. Will ignore any states within
     * the supplied list.
     * @param ignoreList the list of States that should be ignored when checking if any are enabled.
     * @return whether any of the States are enabled.
     */
    public boolean anyStateEnabled( List<Class<? extends State>> ignoreList )
    {
        boolean result = false;

        Iterator<Class<?>> i = states.keySet().iterator();

        while (i.hasNext())
        {
            Class<?> c = i.next();

            if ( ! ignoreList.contains( c )  && states.get(c).isEnabled() )
            {
                result = true;
                break;
            }
        }
        return result;
    }

}
