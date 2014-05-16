package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.IdUtils.ga;

import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.BOMState;
import org.commonjava.maven.ext.manip.state.ManipulationSession;


/**
 * {@link Manipulator} implementation that can alter property sections in a project's pom file.
 * Configuration is stored in a {@link BOMState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "property-manipulator" )
public class PropertyManipulator
    extends AlignmentManipulator
{

    @Requirement
    protected Logger logger;

    protected PropertyManipulator()
    {
    }

    public PropertyManipulator( final Logger logger )
    {
        super (logger);
        this.logger = logger;
    }


    @Override
    public void init( final ManipulationSession session )
    {
        super.init ( session );
        super.baseLogger = this.logger;
    }

    @Override
    protected Map<String, String> loadRemoteBOM (BOMState state)
        throws ManipulationException
    {
        return loadRemoteOverrides( RemoteType.PROPERTY, state.getRemotePropertyMgmt() );
    }

    @Override
    protected void apply (ManipulationSession session, MavenProject project, Model model, Map<String, String> override) throws ManipulationException
    {
        // Only inject the new properties at the top level.
        if ( ! project.isExecutionRoot())
        {
            return;
        }
        logger.info( "Applying property changes to: " + ga(project) );

        model.getProperties().putAll( override );
    }
}
