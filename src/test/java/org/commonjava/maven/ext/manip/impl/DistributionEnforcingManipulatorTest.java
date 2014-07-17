package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator.MAVEN_DEPLOY_ARTIFACTID;
import static org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator.MAVEN_INSTALL_ARTIFACTID;
import static org.commonjava.maven.ext.manip.impl.DistributionEnforcingManipulator.MAVEN_PLUGIN_GROUPID;
import static org.commonjava.maven.ext.manip.state.DistributionEnforcingState.ENFORCE_SYSPROP;
import static org.commonjava.maven.ext.manip.state.DistributionEnforcingState.EnforcingMode.detect;
import static org.commonjava.maven.ext.manip.state.DistributionEnforcingState.EnforcingMode.none;
import static org.commonjava.maven.ext.manip.state.DistributionEnforcingState.EnforcingMode.off;
import static org.commonjava.maven.ext.manip.state.DistributionEnforcingState.EnforcingMode.on;
import static org.commonjava.maven.ext.manip.util.IdUtils.ga;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.resolver.GalleyInfrastructure;
import org.commonjava.maven.ext.manip.state.DistributionEnforcingState;
import org.commonjava.maven.ext.manip.state.DistributionEnforcingState.EnforcingMode;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DistributionEnforcingManipulatorTest
{

    @Test
    public void stateIsEnabledWhenModeIsUnspecified()
        throws Exception
    {
        initTest( null, true );
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
        assertSkip( model, null, false, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenModeIsOff_ParsedPom()
        throws Exception
    {
        final Model model = parseModelResource( "simple-deploy-skip.pom" );

        applyTest( off, model, model );
        assertSkip( model, null, false, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenNoModeIsDetected_ParsedPom()
        throws Exception
    {
        final Model model = parseModelResource( "simple-deploy-skip.pom" );

        applyTest( detect, model, model );
        assertSkip( model, null, false, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenOffModeIsDetected_ParsedPom()
        throws Exception
    {
        final Model model = parseModelResource( "simple-detect-skip.pom" );

        applyTest( detect, model, model );
        assertSkip( model, null, false, true, Boolean.FALSE );
    }

    @Test
    public void projectDeploySkipTurnedOffWhenOffModeIsDetected_InPluginExecution_ParsedPom()
        throws Exception
    {
        final Model model = parseModelResource( "exec-detect-skip.pom" );

        applyTest( detect, model, model );
        assertSkip( model, null, false, true, Boolean.FALSE );
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
            userCliProperties.setProperty( ENFORCE_SYSPROP, mode.name() );
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

    private void assertSkip( final Model model, final String profileId, final boolean managed, final boolean deploy,
                             final boolean state )
    {
        final Build build = model.getBuild();
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
        final List<Project> projects = new ArrayList<Project>();
        projects.add( project );

        session.setManipulatedModels( Collections.singletonMap( ga( model ), model ) );

        final Set<Project> changed = manipulator.applyChanges( projects, session );

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
        final List<Project> projects = new ArrayList<Project>();
        final Map<String, Model> manipulatedModels = new HashMap<String, Model>();
        for ( final Model model : models )
        {
            final Project project = new Project( model );
            projects.add( project );
            manipulatedModels.put( ga( model ), model );
        }

        session.setManipulatedModels( manipulatedModels );

        final Set<Project> changed = manipulator.applyChanges( projects, session );

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

    private Model parseModelResource( final String resourceName )
        throws Exception
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( RESOURCE_BASE + resourceName );

        return new MavenXpp3Reader().read( new FileReader( new File( resource.getPath() ) ) );
    }

    private static final String RESOURCE_BASE = "enforce-skip/";

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
            new GalleyInfrastructure( session, null, null, null, temp.newFolder( "cache-dir" ) );

        final GalleyAPIWrapper wrapper = new GalleyAPIWrapper( galleyInfra );

        manipulator = new DistributionEnforcingManipulator( wrapper );
    }

}
