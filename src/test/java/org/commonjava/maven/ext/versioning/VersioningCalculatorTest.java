package org.commonjava.maven.ext.versioning;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
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
import org.apache.maven.execution.MavenExecutionRequest;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.IOUtil;
import org.commonjava.maven.ext.versioning.fixture.StubRepositorySystem;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VersioningCalculatorTest
{

    private static final String GROUP_ID = "group.id";

    private static final String ARTIFACT_ID = "artifact-id";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private VersionCalculator modder;

    private StubRepositorySystem repoSystem;

    @Before
    public void before()
    {
        repoSystem = new StubRepositorySystem();
        final ConsoleLogger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        modder = new VersionCalculator( repoSystem, logger );
    }

    public void initFailsWithoutSuffixProperty()
        throws Exception
    {
        final VersioningSession session = setupSession( new Properties() );
        assertThat( session.isEnabled(), equalTo( false ) );
    }

    @Test
    public void indempotency()
        throws Exception
    {
        final Properties props = new Properties();

        final String s = "foo";
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
        props.setProperty( VersioningSession.VERSION_SUFFIX_SNAPSHOT_SYSPROP, "true" );
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
        props.setProperty( VersioningSession.VERSION_SUFFIX_SYSPROP, s );
        props.setProperty( VersioningSession.VERSION_SUFFIX_SNAPSHOT_SYSPROP, "true" );
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
        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, s );
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

        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, "foo-0" );
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
        setMetadataVersions( "1.2.GA-foo-3", "1.2.GA-foo-2", "1.2.GA-foo-9" );

        final Properties props = new Properties();

        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, "foo-0" );
        setupSession( props );

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
        setMetadataVersions( "0.0.1", "0.0.2", "0.0.3", "0.0.4", "0.0.5", "0.0.6", "0.0.7", "0.0.7.redhat-1" );

        final Properties props = new Properties();

        props.setProperty( VersioningSession.INCREMENT_SERIAL_SUFFIX_SYSPROP, "redhat-0" );
        setupSession( props );

        final String v = "0.0.7";
        //        final String os = "-redhat-2";
        final String ns = "redhat-2";

        final String result = calculate( v );
        assertThat( result, equalTo( v + "." + ns ) );
    }

    private void setMetadataVersions( final String... versions )
        throws IOException
    {
        final Metadata md = new Metadata();
        final Versioning v = new Versioning();
        md.setVersioning( v );
        v.setVersions( Arrays.asList( versions ) );

        final File mdFile = temp.newFile();
        FileOutputStream stream = null;
        try
        {
            stream = new FileOutputStream( mdFile );
            new MetadataXpp3Writer().write( stream, md );
        }
        finally
        {
            IOUtil.close( stream );
        }

        repoSystem.setMetadataFile( mdFile );
    }

    private String calculate( final String version )
        throws Exception
    {
        return modder.calculate( GROUP_ID, ARTIFACT_ID, version );
    }

    private VersioningSession setupSession( final Properties properties )
    {
        final ArtifactRepository ar =
            new MavenArtifactRepository( "test", "http://repo.maven.apache.org/maven2", new DefaultRepositoryLayout(),
                                         new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy() );

        final MavenExecutionRequest req = new DefaultMavenExecutionRequest().setUserProperties( properties )
                                                                            .setRemoteRepositories( Arrays.asList( ar ) );

        final VersioningSession session = VersioningSession.getInstance();

        session.setRequest( req );

        return session;
    }

}
