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

import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.fixture.TestUtils;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.resolver.GalleyInfrastructure;
import org.commonjava.maven.ext.manip.state.DistributionEnforcingState;
import org.commonjava.maven.ext.manip.state.EnforcingMode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator.MAVEN_DEPLOY_ARTIFACTID;
import static org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator.MAVEN_INSTALL_ARTIFACTID;
import static org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator.MAVEN_PLUGIN_GROUPID;
import static org.commonjava.maven.ext.manip.state.DistributionEnforcingState.ENFORCE_SYSPROP;
import static org.commonjava.maven.ext.manip.state.EnforcingMode.detect;
import static org.commonjava.maven.ext.manip.state.EnforcingMode.none;
import static org.commonjava.maven.ext.manip.state.EnforcingMode.off;
import static org.commonjava.maven.ext.manip.state.EnforcingMode.on;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DistributionEnforcingManipulatorTest
{
    private static final String RESOURCE_BASE = "enforce-skip/";

    @Test
    public void stateIsEnabledWhenModeIsUnspecified()
        throws Exception
    {
        initTest( null, false );
    }

    @Test
    public void stateIsDisabledWhenModeIsNone()
        throws Exception
    {
        initTest( none, false );
    }

    @Test
    public void stateIsEnabledWhenModeIsOn()
        throws Exception
    {
        initTest( on, true );
    }

    @Test
    public void stateIsEnabledWhenModeIsOff()
        throws Exception
    {
        initTest( off, true );
    }

    @Test
    public void stateIsEnabledWhenModeIsDetect()
        throws Exception
    {
        initTest( detect, true );
    }

    @Test
    public void projectUnchangedWhenModeIsNone()
        throws Exception
    {
        final Plugin plugin = new Plugin();
        plugin.setGroupId( MAVEN_PLUGIN_GROUPID );
        plugin.setArtifactId( MAVEN_DEPLOY_ARTIFACTID );
        plugin.setConfiguration( simpleSkipConfig( true ) );

        final Build build = new Build();
        build.addPlugin( plugin );

        final Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( "org.foo" );
        model.setArtifactId( "bar" );
        model.setVersion( "1" );

        model.setBuild( build );

        applyTest( none, model, null );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenModeIsOff()
        throws Exception
    {
        final Plugin plugin = new Plugin();
        plugin.setGroupId( MAVEN_PLUGIN_GROUPID );
        plugin.setArtifactId( MAVEN_DEPLOY_ARTIFACTID );
        plugin.setConfiguration( simpleSkipConfig( true ) );

        final Build build = new Build();
        build.addPlugin( plugin );

        final Model model = new Model();
        model.setModelVersion( "4.0.0" );
        model.setGroupId( "org.foo" );
        model.setArtifactId( "bar" );
        model.setVersion( "1" );

        model.setBuild( build );

        applyTest( off, model, model );
        assertSkip( model, null, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenModeIsOff_ParsedPom()
        throws Exception
    {
        final Model model = TestUtils.resolveModelResource( RESOURCE_BASE, "simple-deploy-skip.pom" );

        applyTest( off, model, model );
        assertSkip( model, null, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenNoModeIsDetected_ParsedPom()
        throws Exception
    {
        final Model model = TestUtils.resolveModelResource( RESOURCE_BASE, "simple-deploy-skip.pom" );

        applyTest( detect, model, model );
        assertSkip( model, null, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenOffModeIsDetected_ParsedPom()
        throws Exception
    {
        final Model model = TestUtils.resolveModelResource( RESOURCE_BASE, "simple-detect-skip.pom" );

        applyTest( detect, model, model );
        assertSkip( model, null, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenOffModeIsDetected_InPluginExecution_ParsedPom()
        throws Exception
    {
        final Model model = TestUtils.resolveModelResource( RESOURCE_BASE, "exec-detect-skip.pom" );

        applyTest( detect, model, model );
        assertSkip( model, null, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOff_InProfile_ModeIsOff_ParsedPom()
        throws Exception
    {
        final Model model = TestUtils.resolveModelResource( RESOURCE_BASE, "profile-deploy-skip.pom" );

        applyTest( off, model, model );
        assertSkip( model, "test", true, Boolean.FALSE );
    }

    private void initTest( final EnforcingMode mode, final boolean enabled )
        throws Exception
    {
        setModeProperty( mode );
        setMavenSession();

        manipulator.init( session );

        final DistributionEnforcingState state = session.getState( DistributionEnforcingState.class );
        assertThat( state.isEnabled(), equalTo( enabled ) );
    }

    private void setModeProperty( final EnforcingMode mode )
    {
        if ( mode != null )
        {
            userCliProperties.setProperty( ENFORCE_SYSPROP.getCurrent(), mode.name() );
        }
    }

    private Object simpleSkipConfig( final boolean enabled )
        throws Exception
    {
        return Xpp3DomBuilder.build( new StringReader( "<configuration><skip>" + enabled + "</skip></configuration>" ) );
    }

    private void setMavenSession()
        throws Exception
    {
        final MavenExecutionRequest req =
            new DefaultMavenExecutionRequest().setUserProperties( userCliProperties )
                                              .setRemoteRepositories( Collections.<ArtifactRepository> emptyList() );

        final PlexusContainer container = new DefaultPlexusContainer();
        final MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );

        session.setMavenSession( mavenSession );

    }

    private void assertSkip( final Model model, final String profileId, final boolean deploy, final boolean state )
    {
        BuildBase build = null;
        if ( profileId != null )
        {
            final List<Profile> profiles = model.getProfiles();
            if ( profiles != null )
            {
                for ( final Profile profile : profiles )
                {
                    if ( profileId.equals( profile.getId() ) )
                    {
                        build = profile.getBuild();
                    }
                }
            }
        }
        else
        {
            build = model.getBuild();
        }

        assertThat( build, notNullValue() );

        final Plugin plugin =
            build.getPluginsAsMap()
                 .get( ga( MAVEN_PLUGIN_GROUPID, deploy ? MAVEN_DEPLOY_ARTIFACTID : MAVEN_INSTALL_ARTIFACTID ) );

        assertThat( plugin, notNullValue() );

        assertThat( plugin.getConfiguration()
                          .toString()
                          .contains( "<skip>" + state + "</skip>" ), equalTo( true ) );
    }

    private void applyTest( final EnforcingMode mode, final Model model, final Model expectChanged )
        throws Exception
    {
        setModeProperty( mode );

        setMavenSession();

        manipulator.init( session );

        final Project project = new Project( model );
        final List<Project> projects = new ArrayList<>();
        projects.add( project );

        final Set<Project> changed = manipulator.applyChanges( projects );

        if ( expectChanged != null )
        {
            assertThat( changed.isEmpty(), equalTo( false ) );
            assertThat( changed.contains( new Project( expectChanged ) ), equalTo( true ) );
        }
        else
        {
            assertThat( changed.isEmpty(), equalTo( true ) );
        }
    }

    @SuppressWarnings( "unused" )
    private void applyTest( final Set<Model> models, final Set<Model> expectChanged )
        throws Exception
    {
        final List<Project> projects = new ArrayList<>();
        final Map<String, Model> manipulatedModels = new HashMap<>();
        for ( final Model model : models )
        {
            final Project project = new Project( model );
            projects.add( project );
            manipulatedModels.put( ga( model ), model );
        }

        final Set<Project> changed = manipulator.applyChanges( projects );

        if ( expectChanged != null && !expectChanged.isEmpty() )
        {
            assertThat( changed.isEmpty(), equalTo( false ) );
            for ( final Model model : expectChanged )
            {
                assertThat( changed.contains( new Project( model ) ), equalTo( true ) );
            }
        }
        else
        {
            assertThat( changed.isEmpty(), equalTo( true ) );
        }
    }

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private ManipulationSession session;

    private DistributionEnforcingManipulator manipulator;

    private Properties userCliProperties;

    @Before
    public void before()
        throws Exception
    {
        userCliProperties = new Properties();
        session = new ManipulationSession();

        final GalleyInfrastructure galleyInfra =
            new GalleyInfrastructure( session.getTargetDir(), session.getRemoteRepositories(),
                            session.getLocalRepository(), session.getSettings(), session.getActiveProfiles(),
                            null, null, null, temp.newFolder( "cache-dir" ) );

        final GalleyAPIWrapper wrapper = new GalleyAPIWrapper( galleyInfra );

        manipulator = new DistributionEnforcingManipulator( );
        FieldUtils.writeField (manipulator, "galleyWrapper", wrapper, true);
    }
}
