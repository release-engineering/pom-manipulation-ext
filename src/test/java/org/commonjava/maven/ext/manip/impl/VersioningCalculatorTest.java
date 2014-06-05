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
package org.commonjava.maven.ext.manip.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.repository.DefaultMirrorSelector;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.fixture.StubTransport;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.resolver.GalleyInfrastructure;
import org.commonjava.maven.ext.manip.resolver.MavenLocationExpander;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.Transport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VersioningCalculatorTest
{

    private static final String GROUP_ID = "group.id";

    private static final String ARTIFACT_ID = "artifact-id";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private TestVersionCalculator modder;

    private ManipulationSession session;

    public void initFailsWithoutSuffixProperty()
        throws Exception
    {
        final VersioningState session = setupSession( new Properties() );
        assertThat( session.isEnabled(), equalTo( false ) );
    }

    @Test
    public void indempotency()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2";

        String result = calculate( v );
        assertThat( result, equalTo( v + "." + s ) );

        result = calculate( result );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applyNonSerialSuffix_NumericVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applyNonSerialSuffix_NonNumericVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2.GA";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySerialSuffix_SPnVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2-SP4";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySerialSuffix_NumericVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applySerialSuffix_NonNumericSuffixInVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2.GA-foo";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "-1" ) );
    }

    @Test
    public void applySerialSuffix_SimpleSuffixProperty()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String originalVersion = "1.0.0.Final";
        final String calcdVersion = "1.0.0.Final-foo-1";

        final String result = calculate( originalVersion );
        assertThat( result, equalTo( calcdVersion ) );
    }

    @Test
    public void applySerialSuffix_NonNumericNonSuffixInVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2.GA-jdcasey";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySerialSuffix_NonNumericVersionTail()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-1";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2.GA";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySerialSuffix_NumericVersionTail_OverwriteExisting()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2";
        final String os = ".foo-1";

        final String result = calculate( v + os );
        assertThat( result, equalTo( v + "." + s ) );
    }

    @Test
    public void applySerialSuffix_NonNumericVersionTail_OverwriteExisting()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String v = "1.2.GA";
        final String os = "-foo-1";

        final String result = calculate( v + os );
        assertThat( result, equalTo( v + "-" + s ) );
    }

    @Test
    public void applySuffixBeforeSNAPSHOT()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        props.setProperty( VersioningState.VERSION_SUFFIX_SNAPSHOT_SYSPROP, "true" );
        setupSession( props );

        final String v = "1.2.GA";
        final String sn = "-SNAPSHOT";

        final String result = calculate( v + sn );
        assertThat( result, equalTo( v + "-" + s + sn ) );
    }

    @Test
    public void applySuffixBeforeSNAPSHOT_OverwriteExisting()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo-2";
        props.setProperty( VersioningState.VERSION_SUFFIX_SYSPROP, s );
        props.setProperty( VersioningState.VERSION_SUFFIX_SNAPSHOT_SYSPROP, "true" );
        setupSession( props );

        final String v = "1.2.GA";
        final String sn = "-SNAPSHOT";
        final String os = "-foo-1";

        final String result = calculate( v + os + sn );
        assertThat( result, equalTo( v + "-" + s + sn ) );
    }

    @Test
    public void applySuffixReplaceSNAPSHOT()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
        setupSession( props );

        final String originalVersion = "1.0.0.Final-foo-SNAPSHOT";
        final String calcdVersion = "1.0.0.Final-foo-1";

        final String result = calculate( originalVersion );
        assertThat( result, equalTo( calcdVersion ) );
    }

    @Test
    public void incrementExistingSerialSuffix()
        throws Exception
    {
        final Properties props = new Properties();

        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "foo-0" );
        setupSession( props );

        final String v = "1.2.GA";
        final String os = "-foo-1";
        final String ns = "foo-2";

        final String result = calculate( v + os );
        assertThat( result, equalTo( v + "-" + ns ) );
    }

    @Test
    public void incrementExistingSerialSuffix_UsingRepositoryMetadata()
        throws Exception
    {
        final Properties props = new Properties();

        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "foo-0" );
        setupSession( props, "1.2.GA-foo-3", "1.2.GA-foo-2", "1.2.GA-foo-9" );

        final String v = "1.2.GA";
        final String os = "-foo-1";
        final String ns = "foo-10";

        final String result = calculate( v + os );
        assertThat( result, equalTo( v + "-" + ns ) );
    }

    @Test
    public void incrementExistingSerialSuffix_UsingRepositoryMetadataWithIrrelevantVersions()
        throws Exception
    {
        final Properties props = new Properties();

        props.setProperty( VersioningState.INCREMENT_SERIAL_SUFFIX_SYSPROP, "redhat-0" );
        setupSession( props, "0.0.1", "0.0.2", "0.0.3", "0.0.4", "0.0.5", "0.0.6", "0.0.7", "0.0.7.redhat-1" );

        final String v = "0.0.7";
        //        final String os = "-redhat-2";
        final String ns = "redhat-2";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "." + ns ) );
    }

    private byte[] setupMetadataVersions( final String... versions )
        throws IOException
    {
        final Metadata md = new Metadata();
        final Versioning v = new Versioning();
        md.setVersioning( v );
        v.setVersions( Arrays.asList( versions ) );

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new MetadataXpp3Writer().write( baos, md );

        return baos.toByteArray();
    }

    private String calculate( final String version )
        throws Exception
    {
        return modder.calculate( GROUP_ID, ARTIFACT_ID, version, session );
    }

    private VersioningState setupSession( final Properties properties, final String... versions )
        throws Exception
    {
        final ArtifactRepository ar =
            new MavenArtifactRepository( "test", "http://repo.maven.apache.org/maven2", new DefaultRepositoryLayout(),
                                         new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );

        final MavenExecutionRequest req =
            new DefaultMavenExecutionRequest().setUserProperties( properties )
                                              .setRemoteRepositories( Arrays.asList( ar ) );

        final PlexusContainer container = new DefaultPlexusContainer();

        final MavenSession mavenSession = new MavenSession( container, null, req, new DefaultMavenExecutionResult() );

        session = new ManipulationSession();
        session.setMavenSession( mavenSession );

        final VersioningState state = new VersioningState( properties );
        session.setState( state );

        byte[] data = null;
        if ( versions.length > 0 )
        {
            data = setupMetadataVersions( versions );
        }

        final Location mdLoc = MavenLocationExpander.EXPANSION_TARGET;
        final Transport mdTrans = new StubTransport( data );

        modder =
            new TestVersionCalculator( new ManipulationSession(), mdLoc, mdTrans, temp.newFolder( "galley-cache" ) );

        return state;
    }

    public static final class TestVersionCalculator
        extends VersionCalculator
    {

        public TestVersionCalculator( final ManipulationSession session )
            throws ManipulationException
        {
            super( new GalleyAPIWrapper( new GalleyInfrastructure( session ) ) );
        }

        public TestVersionCalculator( final ManipulationSession session, final Location mdLoc, final Transport mdTrans,
                                      final File cacheDir )
            throws ManipulationException
        {
            super( new GalleyAPIWrapper( new GalleyInfrastructure( session, new DefaultMirrorSelector(), mdLoc,
                                                                   mdTrans, cacheDir ) ) );
        }

        @Override
        public String calculate( final String groupId, final String artifactId, final String originalVersion,
                                 final ManipulationSession session )
            throws ManipulationException
        {
            return super.calculate( groupId, artifactId, originalVersion, session );
        }

    }

}
