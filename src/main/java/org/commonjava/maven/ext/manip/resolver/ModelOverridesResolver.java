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
/**
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.manip.resolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.building.ModelBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.sonatype.aether.impl.ArtifactResolver;
import org.w3c.dom.Node;

/**
 * Class to resolve artifact descriptors (pom files) from a maven repository
 */
@Component( role = ModelOverridesResolver.class )
public class ModelOverridesResolver
{
    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactResolver resolver;

    @Requirement
    private ModelBuilder modelBuilder;

    @Requirement
    private PomReaderWrapper pomReader;

    /**
     * Protected constructor for component instantiation/injection
     */
    protected ModelOverridesResolver()
    {

    }

    public Map<String, String> getRemoteDependencyVersionOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving dependency management GAV: " + gav );

        final Map<String, String> versionOverrides = new HashMap<String, String>();
        try
        {
            final MavenPomView pomView = pomReader.readPomView( ProjectVersionRef.parse( gav ) );

            // TODO: active profiles!
            final List<DependencyView> deps = pomView.getAllManagedDependencies();
            if ( deps == null || deps.isEmpty() )
            {
                throw new ManipulationException(
                                                 "Attempting to align to a BOM that does not have a dependencyManagement section" );
            }

            for ( final DependencyView dep : deps )
            {
                versionOverrides.put( dep.asProjectRef()
                                         .toString(), dep.getVersion() );
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

    public Map<String, String> getRemotePropertyMappingOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving remote property mapping POM: " + gav );

        final Map<String, String> versionOverrides = new HashMap<String, String>();
        try
        {
            final MavenPomView pomView = pomReader.readPomView( ProjectVersionRef.parse( gav ) );

            // TODO: active profiles!
            // TODO: Provide method for retrieving property map from pomView, instead of using this low-level api.
            final List<Node> properties = pomView.resolveXPathToAggregatedNodeList( "//properties", true, -1 );

            for ( final Node prop : properties )
            {
                // TODO: cleanup of text?
                versionOverrides.put( prop.getNodeName(), prop.getTextContent()
                                                              .trim() );
            }
        }
        catch ( final GalleyMavenException e )
        {
            throw new ManipulationException( "Unable to resolve: %s", e, gav );
        }
        finally
        {
        }

        logger.debug( "Returning override of " + versionOverrides );
        return versionOverrides;
    }

    public Map<String, String> getRemotePluginVersionOverrides( final String gav, final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Resolving remote plugin management POM: " + gav );

        final Map<String, String> versionOverrides = new HashMap<String, String>();
        try
        {
            final MavenPomView pomView = pomReader.readPomView( ProjectVersionRef.parse( gav ) );

            // TODO: active profiles!
            final List<PluginView> plugins = pomView.getAllManagedBuildPlugins();
            if ( plugins == null || plugins.isEmpty() )
            {
                throw new ManipulationException(
                                                 "Attempting to align to a BOM that does not have a pluginManagement section" );
            }

            for ( final PluginView plugin : plugins )
            {
                versionOverrides.put( plugin.asProjectRef()
                                            .toString(), plugin.getVersion() );

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
