package org.commonjava.maven.ext.manip.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.repository.DefaultMirrorSelector;
import org.apache.maven.settings.Mirror;
import org.commonjava.maven.galley.model.Location;
import org.junit.Test;

public class MavenLocationExpanderTest
{

    @Test
    public void mirrorAdjustsLocationURLs()
        throws Exception
    {
        final Mirror mirror = new Mirror();
        mirror.setId( "test-mirror" );
        mirror.setMirrorOf( "*" );
        mirror.setUrl( "http://nowhere.com" );

        final ArtifactRepositoryLayout layout = new DefaultRepositoryLayout();

        final ArtifactRepositoryPolicy snapshots =
            new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY,
                                          ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );

        final ArtifactRepositoryPolicy releases =
            new ArtifactRepositoryPolicy( true, ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER,
                                          ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN );

        final File localRepo = File.createTempFile( "local.repo.", ".dir" );
        localRepo.deleteOnExit();

        final ArtifactRepository local =
            new MavenArtifactRepository( "local", localRepo.toURI()
                                                           .toString(), layout, snapshots, releases );

        final ArtifactRepository remote =
            new MavenArtifactRepository( "remote", "http:///repo.maven.apache.org/maven2", layout, snapshots, releases );

        final MavenLocationExpander ex =
            new MavenLocationExpander( Collections.<Location> emptyList(),
                                       Collections.<ArtifactRepository> singletonList( remote ), local,
                                       new DefaultMirrorSelector(), Collections.<Mirror> singletonList( mirror ) );

        final List<Location> result = ex.expand( MavenLocationExpander.EXPANSION_TARGET );

        assertThat( result.size(), equalTo( 2 ) );

        final Iterator<Location> iterator = result.iterator();
        Location loc = iterator.next();

        assertThat( loc.getName(), equalTo( local.getId() ) );
        assertThat( loc.getUri(), equalTo( local.getUrl() ) );

        loc = iterator.next();

        assertThat( loc.getName(), equalTo( mirror.getId() ) );
        assertThat( loc.getUri(), equalTo( mirror.getUrl() ) );
    }

}
