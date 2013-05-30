package org.commonjava.maven.ext.versioning;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

@Component( role = AbstractMavenLifecycleParticipant.class, hint = "versioning" )
public class VersioningLifecycleParticipant
    extends AbstractMavenLifecycleParticipant
{

    @Requirement
    private Logger logger;

    @Requirement
    private VersioningModifier modifier;

    public VersioningLifecycleParticipant()
    {
    }

    public VersioningLifecycleParticipant( final VersioningModifier modifier, final Logger logger )
    {
        this.modifier = modifier;
        this.logger = logger;
    }

    @Override
    public void afterProjectsRead( final MavenSession session )
        throws MavenExecutionException
    {
        final Set<MavenProject> modified = modifier.apply( session.getProjects(), session.getUserProperties() );
        for ( final MavenProject project : modified )
        {
            rewritePom( project );
        }

        super.afterProjectsRead( session );
    }

    private void rewritePom( final MavenProject project )
        throws MavenExecutionException
    {
        final File pom = project.getFile();
        Writer writer = null;
        try
        {
            writer = WriterFactory.newXmlWriter( pom );
            new MavenXpp3Writer().write( writer, project.getOriginalModel() );
        }
        catch ( final IOException e )
        {
            throw new MavenExecutionException( "Failed to rewrite project: " + project + " (POM: " + pom
                + "). Reason: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

}
