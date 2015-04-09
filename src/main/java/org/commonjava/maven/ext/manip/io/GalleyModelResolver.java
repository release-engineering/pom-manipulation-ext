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

package org.commonjava.maven.ext.manip.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.util.UrlUtils;

public class GalleyModelResolver
    implements ModelResolver
{

    private final GalleyAPIWrapper galleyWrapper;

    public GalleyModelResolver( final GalleyAPIWrapper galleyWrapper )
    {
        this.galleyWrapper = galleyWrapper;
    }

    @Override
    public ModelSource resolveModel( final String groupId, final String artifactId, final String version )
        throws UnresolvableModelException
    {
        Transfer transfer;
        final ArtifactRef ar = new ProjectVersionRef( groupId, artifactId, version ).asPomArtifact();
        try
        {
            transfer =
                galleyWrapper.resolveArtifact( ar );
        }
        catch ( final TransferException e )
        {
            throw new UnresolvableModelException( "Failed to resolve POM: " + e.getMessage(), groupId, artifactId,
                                                  version, e );
        }
        if ( transfer == null )
        {
            throw new UnresolvableModelException( "Failed to resolve POM: " + ar, groupId, artifactId, version );
        }

        return new TransferModelSource( transfer );
    }

    @Override
    public void addRepository( final Repository repository )
        throws InvalidRepositoryException
    {
        // disregard.
        // FIXME: Is this dangerous, given the legacy of pom-declared repos??
    }

    @Override
    public ModelResolver newCopy()
    {
        // no state here, so we can keep this instance.
        return this;
    }

    private static final class TransferModelSource
        implements ModelSource
    {

        private final Transfer transfer;

        public TransferModelSource( final Transfer transfer )
        {
            this.transfer = transfer;
        }

        @Override
        public InputStream getInputStream()
            throws IOException
        {
            return transfer.openInputStream();
        }

        @Override
        public String getLocation()
        {
            String location = null;
            try
            {
                location = UrlUtils.buildUrl( transfer.getLocation()
                                                      .getUri(), transfer.getPath() );
            }
            catch ( final MalformedURLException e )
            {
                location = transfer.toString();
            }

            return location;
        }

    }

}
