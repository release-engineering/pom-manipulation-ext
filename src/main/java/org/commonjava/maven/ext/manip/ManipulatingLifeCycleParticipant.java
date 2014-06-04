package org.commonjava.maven.ext.manip;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "manipulation" )
public class ManipulatingLifeCycleParticipant
    extends AbstractMavenLifecycleParticipant
{

    @Requirement
    private ManipulationSession session;

    @Override
    public void afterProjectsRead( final MavenSession mavenSession )
        throws MavenExecutionException
    {
        final ManipulationException error = session.getError();
        if ( error != null )
        {
            throw new MavenExecutionException( "POM Manipulation failed: " + error.getMessage(), error );
        }

        super.afterProjectsRead( mavenSession );
    }

}
