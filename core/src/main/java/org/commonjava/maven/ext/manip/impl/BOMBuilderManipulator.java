/*
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
package org.commonjava.maven.ext.manip.impl;

import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.BOMInjectingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

/**
 * Simple manipulator that will look for all module artifacts and construct a BOM that is deployed with the root artifact. It has a predictable naming
 * scheme making it useful in automated scenarios. Configuration consists of activation, documented in {@link BOMInjectingState}.
 */
@Component( role = Manipulator.class, hint = "bom-builder" )
public class BOMBuilderManipulator
    implements Manipulator
{
    @Requirement
    private PomIO pomIO;

    // http://www.mojohaus.org/build-helper-maven-plugin/attach-artifact-mojo.html
    private static final String BUILD_HELPER_GID = "org.codehaus.mojo";

    private static final String BUILD_HELPER_AID = "build-helper-maven-plugin";

    private static final String BUILD_HELPER_VERSION = "3.0.0";

    private static final String BUILD_HELPER_COORD = ga( BUILD_HELPER_GID, BUILD_HELPER_AID );

    private static final String BUILD_HELPER_GOAL = "attach-artifact";

    private static final String BOM_ARTIFACT = "generated-bom.xml";

    private static final String ID = "pme-bom-builder";

    private static final String PHASE = "package";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        session.setState( new BOMInjectingState( session.getUserProperties() ) );
    }

    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * If enabled, grab the execution root pom (which will be the topmost POM in terms of directory structure). Check for the
     * presence of the build-helper-maven-plugin in the base build (/project/build/plugins/). Inject a new plugin execution for creating project
     * sources if this plugin has not already been declared in the base build section.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final BOMInjectingState state = session.getState( BOMInjectingState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        List<Dependency> projectArtifacts = getArtifacts( projects, session);

        for ( final Project project : projects )
        {
            if ( project.isExecutionRoot() )
            {
                logger.info( "Examining {} to add BOM generation.", project );

                final Model model = project.getModel();
                Build build = model.getBuild();
                if ( build == null )
                {
                    build = new Build();
                    model.setBuild( build );
                }

                final Model bomModel = new Model();
                bomModel.setGroupId( model.getGroupId() );
                bomModel.setVersion( model.getVersion() );
                bomModel.setArtifactId( model.getArtifactId() );
                bomModel.setPackaging( "pom" );
                DependencyManagement dm = new DependencyManagement();
                dm.setDependencies( projectArtifacts );
                bomModel.setDependencyManagement( dm );
                pomIO.writeModel( bomModel, new File( project.getPom().getParentFile(), BOM_ARTIFACT ) );

                final Map<String, Plugin> pluginMap = build.getPluginsAsMap();

                Plugin plugin;

                // Add to the plugin if it already exists...
                if ( pluginMap.containsKey( BUILD_HELPER_COORD ) )
                {
                    plugin = pluginMap.get( BUILD_HELPER_COORD );
                }
                else
                {
                    plugin = new Plugin();
                    plugin.setGroupId( BUILD_HELPER_GID );
                    plugin.setArtifactId( BUILD_HELPER_AID );
                    plugin.setVersion( BUILD_HELPER_VERSION );
                }

                // ... except if we have already added it before and we are rerunning
                if ( !( plugin.getExecutions() != null && plugin.getExecutions().contains( ID ) ) )
                {
                    final PluginExecution execution = new PluginExecution();
                    execution.setId( ID );
                    execution.setPhase( PHASE );
                    execution.setGoals( Collections.singletonList( BUILD_HELPER_GOAL ) );

                    final Xpp3Dom xml = new Xpp3Dom( "configuration" );
                    final Xpp3Dom artifacts = new Xpp3Dom( "artifacts" );
                    final Xpp3Dom artifact = new Xpp3Dom( "artifact" );
                    final Xpp3Dom file = new Xpp3Dom( "file" );
                    final Xpp3Dom type = new Xpp3Dom( "type" );
                    final Xpp3Dom classifier = new Xpp3Dom( "classifier" );

                    final Xpp3Dom runOnlyAtExecutionRoot = new Xpp3Dom( "runOnlyAtExecutionRoot" );
                    runOnlyAtExecutionRoot.setValue( "true" );
                    xml.addChild( runOnlyAtExecutionRoot );

                    xml.addChild( artifacts );
                    artifacts.addChild( artifact );
                    artifact.addChild( file );
                    artifact.addChild( type );
                    artifact.addChild( classifier );

                    file.setValue( BOM_ARTIFACT );
                    // TODO: Should we deploy this as xml or pom?
                    type.setValue( "pom" );
                    classifier.setValue( "bom" );

                    execution.setConfiguration( xml );
                    plugin.addExecution( execution );

                    build.addPlugin( plugin );
                }
                else
                {
                    logger.debug( "Plugin collection already contains plugin with execution of " + ID );
                }

                return Collections.singleton( project );
            }
        }

        return Collections.emptySet();
    }


    // TODO: This will grab every module ; so those modules activated under profiles will also get included
    public List<Dependency> getArtifacts( List<Project> projects, ManipulationSession session )
    {
        List<Dependency> results = new ArrayList<>(  );

        for ( Project p : projects )
        {
            Dependency d = new Dependency();
            d.setArtifactId( p.getArtifactId() );
            d.setGroupId( p.getGroupId() );
            d.setVersion( p.getVersion() );
            d.setType( p.getModel().getPackaging() );
            results.add( d );
        }
        return results;
    }

    @Override
    public int getExecutionIndex()
    {
        return 82;
    }

}
