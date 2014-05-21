package org.commonjava.maven.ext.manip.resolver;

import org.apache.maven.execution.MavenSession;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

/**
 * Represents a piece of extension infrastructure that gets initialized when the {@link MavenSession} becomes available.
 * 
 * @author jdcasey
 */
public interface ExtensionInfrastructure
{

    void init( ManipulationSession session )
        throws ManipulationException;

}
