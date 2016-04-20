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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.DependencyView;
import org.commonjava.maven.galley.maven.model.view.MavenPomView;
import org.commonjava.maven.galley.maven.model.view.PluginView;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Class to resolve artifact descriptors (pom files) from a maven repository
 */
@Component( role = ModelIO.class )
public class ModelIO
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private GalleyAPIWrapper galleyWrapper;


    /**
     * Read the raw model (equivalent to the pom file on disk) from a given GAV.
     *
     * @param ref the ProjectVersion to read.
     * @return the Maven Model for the GAV
     * @throws ManipulationException if an error occurs.
     */
    public Model resolveRawModel( final ProjectVersionRef ref )
                    throws ManipulationException
    {
        Transfer transfer;
        try
        {
            transfer = galleyWrapper.resolveArtifact( ref.asPomArtifact() );
        }
        catch ( final TransferException e )
        {
            throw new ManipulationException( "Failed to resolve POM: %s.\n--> %s", e, ref, e.getMessage() );
        }
        if ( transfer == null )
        {
            throw new ManipulationException( "Failed to resolve POM: " + ref.asPomArtifact() );
        }

        InputStream in = null;
        try
        {
            in = transfer.openInputStream();
            return new MavenXpp3Reader().read( in );
        }
        catch ( final IOException | XmlPullParserException e )
        {
            throw new ManipulationException( "Failed to build model for POM: %s.\n--> %s", e, ref, e.getMessage() );
        }
        finally
        {
            closeQuietly( in );
        }
    }

    /**
     * Read the raw file from a given GAVTC (GAV + Type and Classifier). Useful if we need to read
     * a remote file.
     *
     * @param ref the ArtifactRef to read.
     * @return the file for the GAVTC
     * @throws ManipulationException if an error occurs.
     */
    public File resolveRawFile( final ArtifactRef ref )
                    throws ManipulationException
    {
        Transfer transfer;
        try
        {
            transfer = galleyWrapper.resolveArtifact( ref );
        }
        catch ( final TransferException e )
        {
            throw new ManipulationException( "Failed to resolve POM: %s.\n--> %s", e, ref, e.getMessage() );
        }
        if ( transfer == null )
        {
            throw new ManipulationException( "Failed to resolve POM: " + ref.asPomArtifact() );
        }

        return transfer.getDetachedFile();
    }

    public Map<ArtifactRef, String> getRemoteDependencyVersionOverrides( final ProjectVersionRef ref )
                    throws ManipulationException
    {
        logger.debug( "Resolving dependency management GAV: " + ref );

        final Map<ArtifactRef, String> versionOverrides = new LinkedHashMap<>();
        try
        {
            final MavenPomView pomView = galleyWrapper.readPomView( ref );

            // TODO: active profiles!
            final List<DependencyView> deps = pomView.getAllManagedDependencies();
            if ( deps == null || deps.isEmpty() )
            {
                logger.warn( "Attempting to align to a BOM that does not have a dependencyManagement section" );
            }
            else
            {
                for ( final DependencyView dep : deps )
                {
                    versionOverrides.put( dep.asArtifactRef(), dep.getVersion() );
                    logger.debug( "Added version override for: " + dep.asProjectRef().toString() + ":" + dep.getVersion() );
                }
            }
        }
        catch ( final GalleyMavenException e )
        {
            throw new ManipulationException( "Unable to resolve: %s", e, ref );
        }

        return versionOverrides;
    }

    public Properties getRemotePropertyMappingOverrides( final ProjectVersionRef ref )
                    throws ManipulationException
    {
        logger.debug( "Resolving remote property mapping POM: " + ref );

        final Model m = resolveRawModel( ref );

        logger.debug( "Returning override of " + m.getProperties() );

        return m.getProperties();
    }

    /**
     * Return remote Plugins to override
     * @param ref the remote reference to resolve.
     * @param userProperties a collection of properties to ignore when resolving the remote plugin property expressions.
     * @return a map containing ProjectRef to Plugins
     * @throws ManipulationException if an error occurs
     */
    public Map<ProjectRef, Plugin> getRemotePluginVersionOverrides( final ProjectVersionRef ref,
                                                                    Properties userProperties )
                    throws ManipulationException
    {
        logger.debug( "Resolving remote plugin management POM: " + ref );
        final Map<ProjectRef, Plugin> pluginOverrides = new HashMap<>();
        final Map<ProjectRef, ProjectVersionRef> pluginOverridesPomView = new HashMap<>();

        final Model m = resolveRawModel( ref );

        try
        {
            final MavenPomView pomView = galleyWrapper.readPomView( ref );
            final List<PluginView> deps = pomView.getAllManagedBuildPlugins();

            for ( final PluginView p : deps )
            {
                pluginOverridesPomView.put( p.asProjectRef(), p.asProjectVersionRef() );
            }
        }
        catch ( GalleyMavenException e )
        {
            throw new ManipulationException( "Unable to resolve: %s", e, ref );
        }

        logger.debug( "Found pluginOverridesResolvedVersions {} " + pluginOverridesPomView );

        // The list of pluginOverridesPomView may be larger than those in current model pluginMgtm. Dummy up an extra
        // set of plugins with versions to handle those.
        for (ProjectRef pr : pluginOverridesPomView.keySet())
        {
            Plugin p = new Plugin();
            p.setArtifactId( pr.getArtifactId() );
            p.setGroupId( pr.getGroupId() );
            p.setVersion( pluginOverridesPomView.get( pr ).getVersionString() );

            pluginOverrides.put( pr, p );

            logger.debug( "Added plugin override for: " + pr.toString() + ":" + p.getVersion() );
        }

        // TODO: active profiles!
        if ( m.getBuild() != null && m.getBuild().getPluginManagement() != null )
        {
            logger.debug( "Returning override of " + m.getBuild().getPluginManagement().getPlugins() );

            Iterator<Plugin> plit = m.getBuild().getPluginManagement().getPlugins().iterator();

            while ( plit.hasNext() )
            {
                Plugin p = plit.next();
                ProjectRef pr = new SimpleProjectRef( p.getGroupId(), p.getArtifactId() );

                if ( p.getVersion() != null && (p.getVersion().startsWith( "${" ) || p.getVersion().length() == 0 ))
                {
                    // Property reference to something in the remote pom. Resolve and inline it now.
                    String newVersion = resolveProperty( userProperties, m.getProperties(), p.getVersion() );

                    // TODO: Complete replacement with PomView
                    if ( newVersion.startsWith("${") || newVersion.length() == 0)
                    {
                        // Use PomView as that contains a pre-resolved list of plugins.
                        newVersion = pluginOverridesPomView.get( pr ).getVersionString();
                    }

                    logger.debug( "Replacing plugin override version " + p.getVersion() +
                                                  " with " + newVersion );
                     p.setVersion( newVersion );
                }
                pluginOverrides.put( pr, p );

                // If we have a configuration block, as per with plugin versions ensure we
                // resolve any properties.
                if ( p.getConfiguration() != null )
                {
                    processChildren( userProperties, m, (Xpp3Dom) p.getConfiguration() );
                }

                if ( p.getExecutions() != null )
                {
                    List<PluginExecution> exes = p.getExecutions();

                    for ( PluginExecution pe : exes )
                    {
                        if ( pe.getConfiguration() != null )
                        {
                            processChildren( userProperties, m, (Xpp3Dom) pe.getConfiguration() );
                        }
                    }
                }

                if ( p.getDependencies() != null)
                {
                    for ( Dependency d : p.getDependencies())
                    {
                        if ( ! isEmpty(d.getVersion()) && d.getVersion().startsWith( "${" ) )
                        {
                            logger.debug( "Processing dependency {} and updating with {} ", d,
                                          resolveProperty( userProperties, m.getProperties(), d.getVersion() ) );
                            d.setVersion( resolveProperty( userProperties, m.getProperties(), d.getVersion() ) );

                        }
                    }
                }

                logger.debug( "Added plugin override for: " + pr.toString() + ":" + p.getVersion() +
                                              " with configuration\n" + p.getConfiguration() + " and executions "
                                              + p.getExecutions() );
            }
        }
        else
        {
            throw new ManipulationException(
                            "Attempting to align to a BOM that does not have a pluginManagement section" );
        }
        return pluginOverrides;
    }

    /**
     * Recursively process the DOM elements to inline any property values from the model.
     * @param userProperties
     * @param model
     * @param parent
     */
    private void processChildren( Properties userProperties, Model model, Xpp3Dom parent )
    {
        for ( int i = 0; i < parent.getChildCount(); i++ )
        {
            Xpp3Dom child = parent.getChild( i );

            if ( child.getChildCount() > 0 )
            {
                processChildren( userProperties, model, child );
            }
            if ( child.getValue() != null && child.getValue().startsWith( "${" ) )
            {
                String replacement = resolveProperty( userProperties, model.getProperties(), child.getValue() );

                if ( replacement != null && !replacement.isEmpty() )
                {
                    logger.debug( "Replacing child value " + child.getValue() + " with " + replacement );
                    child.setValue( replacement );
                }
            }

        }
    }

    /**
     * Recursively resolve a property value.
     *
     * @param userProperties
     * @param p
     * @param key
     * @return the value of the key
     */
    private String resolveProperty( Properties userProperties, Properties p, String key )
    {
        String result = "";
        String child = key.substring( 2, key.length() - 1 );

        if ( p.containsKey( child ) && !userProperties.containsKey( child ) )
        {
            result = p.getProperty( child );

            if ( result.startsWith( "${" ) )
            {
                result = resolveProperty( userProperties, p, result );
            }
        }
        return result;
    }
}
