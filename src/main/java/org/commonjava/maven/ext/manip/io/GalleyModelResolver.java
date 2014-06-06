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

import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Transfer;

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
        try
        {
            transfer =
                galleyWrapper.resolveArtifact( new ProjectVersionRef( groupId, artifactId, version ).asPomArtifact() );
        }
        catch ( final TransferException e )
        {
            throw new UnresolvableModelException( "Failed to resolve POM: " + e.getMessage(), groupId, artifactId,
                                                  version, e );
        }

        return new FileModelSource( transfer.getDetachedFile() );
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

}
