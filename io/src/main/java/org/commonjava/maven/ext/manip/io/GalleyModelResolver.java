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
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
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
        final ArtifactRef ar = new SimpleProjectVersionRef( groupId, artifactId, version ).asPomArtifact();
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
