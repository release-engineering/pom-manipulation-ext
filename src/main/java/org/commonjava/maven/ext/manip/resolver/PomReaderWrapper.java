package org.commonjava.maven.ext.manip.resolver;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenMetadataView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.SimpleLocation;

/**
 * Wraps the galley-maven APIs with the plumbing necessary to resolve using the repositories defined for the maven build.
 * 
 * @author jdcasey
 */
@Component( role = PomReaderWrapper.class )
public class PomReaderWrapper
{

    private static final List<Location> MAVEN_REPOS = new ArrayList<Location>()
    {
        {
            add( new SimpleLocation( "maven:repos" ) );
        }

        private static final long serialVersionUID = 1L;
    };

    @Requirement
    private GalleyInfrastructure infra;

    public MavenPomView readPomView( final ProjectVersionRef ref )
        throws GalleyMavenException
    {
        return infra.getPomReader()
                    .read( ref, MAVEN_REPOS );
    }

    public MavenMetadataView readMetadataView( final ProjectRef ref )
        throws GalleyMavenException
    {
        return infra.getMetadataReader()
                    .getMetadata( ref, MAVEN_REPOS );
    }

}
