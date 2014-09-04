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

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to resolve artifact descriptors (pom files) from a maven repository
 */
@Component( role = ModelIO.class )
public class ModelIO
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private ModelBuilder modelBuilder;

    @Requirement
    private GalleyAPIWrapper galleyWrapper;

    /**
     * Protected constructor for component instantiation/injection
     */
    protected ModelIO()
    {

    }

    /**
     * Read the raw model (equivalent to the pom file on disk) from a given GAV.
     *
     * @param gav
     * @return
     * @throws ManipulationException
     */
    public Model resolveRawModel( final String gav )
        throws ManipulationException
    {
        final ProjectVersionRef ref = ProjectVersionRef.parse( gav );

        Transfer transfer;
        try
        {
            transfer = galleyWrapper.resolveArtifact( ref.asPomArtifact() );
        }
        catch ( final TransferException e )
        {
            throw new ManipulationException( "Failed to resolve POM: %s.\n--> %s", e, ref, e.getMessage() );
        }

        InputStream in = null;
        try
        {
            in = transfer.openInputStream();
            return new MavenXpp3Reader().read( in );
        }
        catch ( final IOException e )
        {
            throw new ManipulationException( "Failed to build model for POM: %s.\n--> %s", e, ref, e.getMessage() );
        }
        catch ( final XmlPullParserException e )
        {
            throw new ManipulationException( "Failed to build model for POM: %s.\n--> %s", e, ref, e.getMessage() );
        }
        finally
        {
            closeQuietly( in );
        }
    }

    public Model readEffectiveModel( final String gav )
        throws ManipulationException
    {
        final DefaultModelBuildingRequest mbr = new DefaultModelBuildingRequest();
        mbr.setModelResolver( new GalleyModelResolver( galleyWrapper ) );
        mbr.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );

        try
        {
            final ModelBuildingResult result = modelBuilder.build( mbr );

            // TODO: Report warnings...

            return result.getEffectiveModel();
        }
        catch ( final ModelBuildingException e )
        {
            throw new ManipulationException( "Failed to build effective Model for: %s.\n--> %s", e, gav, e.getMessage() );
        }
    }

    public Map<ProjectRef, String> getRemoteDependencyVersionOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving dependency management GAV: " + gav );

        final Map<ProjectRef, String> versionOverrides = new LinkedHashMap<ProjectRef, String>();
        try
        {
            final MavenPomView pomView = galleyWrapper.readPomView( ProjectVersionRef.parse( gav ) );

            // TODO: active profiles!
            final List<DependencyView> deps = pomView.getAllManagedDependencies();
            if ( deps == null || deps.isEmpty() )
            {
                throw new ManipulationException(
                                                 "Attempting to align to a BOM that does not have a dependencyManagement section" );
            }

            for ( final DependencyView dep : deps )
            {
                versionOverrides.put( dep.asVersionlessArtifactRef(), dep.getVersion() );
                logger.debug( "Added version override for: " + dep.asProjectRef()
                                                                  .toString() + ":" + dep.getVersion() );
            }
        }
        catch ( final GalleyMavenException e )
        {
            throw new ManipulationException( "Unable to resolve: %s", e, gav );
        }

        return versionOverrides;
    }

    public Properties getRemotePropertyMappingOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving remote property mapping POM: " + gav );

        final Model m = resolveRawModel( gav );

        logger.debug( "Returning override of " + m.getProperties() );

        return m.getProperties();
    }

    public Map<ProjectRef, String> getRemotePluginVersionOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving remote plugin management POM: " + gav );

        final Map<ProjectRef, String> versionOverrides = new HashMap<ProjectRef, String>();
        try
        {
            final MavenPomView pomView = galleyWrapper.readPomView( ProjectVersionRef.parse( gav ) );

            // TODO: active profiles!
            final List<PluginView> plugins = pomView.getAllManagedBuildPlugins();
            if ( plugins == null || plugins.isEmpty() )
            {
                throw new ManipulationException(
                                                 "Attempting to align to a BOM that does not have a pluginManagement section" );
            }

            // Iterate in reverse order so that plugins inherited from the closest parent win.
            ListIterator<PluginView> prt = plugins.listIterator( plugins.size() );
            while (prt.hasPrevious())
            {
                PluginView plugin = prt.previous();
                versionOverrides.put( plugin.asProjectRef(), plugin.getVersion() );

                logger.debug( "Added version override for: " + plugin.asProjectRef()
                                                                     .toString() + ":" + plugin.getVersion() );
            }
        }
        catch ( final GalleyMavenException e )
        {
            throw new ManipulationException( "Unable to resolve: %s", e, gav );
        }

        return versionOverrides;
    }

}
