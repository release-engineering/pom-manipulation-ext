package org.commonjava.maven.ext.manip.resolver;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DocRef;
import org.commonjava.maven.galley.maven.model.view.MavenMetadataView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.MavenXmlView;
import org.commonjava.maven.galley.maven.parse.GalleyMavenXMLException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.model.Transfer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Wraps the galley-maven APIs with the plumbing necessary to resolve using the repositories defined for the maven build.
 * 
 * @author jdcasey
 */
@Component( role = GalleyAPIWrapper.class )
public class GalleyAPIWrapper
{

    private static final List<Location> MAVEN_REPOS = new ArrayList<Location>()
    {
        {
            add( MavenLocationExpander.EXPANSION_TARGET );
        }

        private static final long serialVersionUID = 1L;
    };

    @Requirement( role = ExtensionInfrastructure.class, hint = "galley" )
    private GalleyInfrastructure infra;

    protected GalleyAPIWrapper()
    {
    }

    public GalleyAPIWrapper( final GalleyInfrastructure infra )
    {
        this.infra = infra;
    }

    public Document parseXml( final String xml )
        throws GalleyMavenXMLException
    {
        return infra.getXml()
                    .parseDocument( xml, new ByteArrayInputStream( xml.getBytes() ) );
    }

    public MavenXmlView<ProjectRef> parseXmlView( final String xml )
        throws GalleyMavenXMLException
    {
        final Document document = infra.getXml()
                                       .parseDocument( xml, new ByteArrayInputStream( xml.getBytes() ) );

        final DocRef<ProjectRef> ref = new DocRef<ProjectRef>( new ProjectRef( "unknown", "unknown" ), xml, document );
        return new MavenXmlView<ProjectRef>( Collections.singletonList( ref ), infra.getXPath(), infra.getXml() );
    }

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

    public Transfer resolveArtifact( final ArtifactRef asPomArtifact )
        throws TransferException
    {
        return infra.getArtifactManager()
                    .retrieveFirst( MAVEN_REPOS, asPomArtifact );
    }

    public String toXML( final Node config, final boolean includeXmlDeclaration )
    {
        return infra.getXml()
                    .toXML( config, includeXmlDeclaration );
    }

}
