package org.commonjava.maven.ext.manip.state;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.impl.Manipulator;
import org.sonatype.aether.RepositorySystemSession;

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

    public RepositorySystemSession getRepositorySystemSession()
    {
        return mavenSession == null ? null : mavenSession.getRepositorySession();
    }

    private final Map<Class<?>, Object> states = new HashMap<Class<?>, Object>();

    private MavenSession mavenSession;

    public void setState( final Object state )
    {
        states.put( state.getClass(), state );
    }

    public <T> T getState( final Class<T> stateType )
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

    private final Set<String> changedGAs = new HashSet<String>();

    private Map<String, Model> rawModels;

    private List<MavenProject> projects;

    public Set<String> getChangedGAs()
    {
        return changedGAs;
    }

    public void setManipulatedModels( final Map<String, Model> rawModels )
    {
        this.rawModels = rawModels;
    }

    public Map<String, Model> getManipulatedModels()
    {
        return rawModels;
    }

    public void addChangedGA( final String ga )
    {
        changedGAs.add( ga );
    }

    public Properties getUserProperties()
    {
        return mavenSession == null ? new Properties() : mavenSession.getRequest()
                                                                     .getUserProperties();
    }

    public void setProjectInstances( final List<MavenProject> projects )
    {
        this.projects = projects;
    }

    public List<MavenProject> getProjectInstances()
    {
        return projects;
    }

}
