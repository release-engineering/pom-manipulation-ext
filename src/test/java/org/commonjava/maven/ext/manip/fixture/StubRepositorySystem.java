package org.commonjava.maven.ext.manip.fixture;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.SyncContext;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.deployment.DeployRequest;
import org.sonatype.aether.deployment.DeployResult;
import org.sonatype.aether.deployment.DeploymentException;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.installation.InstallRequest;
import org.sonatype.aether.installation.InstallResult;
import org.sonatype.aether.installation.InstallationException;
import org.sonatype.aether.metadata.Metadata;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.resolution.MetadataRequest;
import org.sonatype.aether.resolution.MetadataResult;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.resolution.VersionRequest;
import org.sonatype.aether.resolution.VersionResolutionException;
import org.sonatype.aether.resolution.VersionResult;

public class StubRepositorySystem
    implements RepositorySystem
{

    private File metadataFile;

    @Override
    public VersionRangeResult resolveVersionRange( final RepositorySystemSession session,
                                                   final VersionRangeRequest request )
        throws VersionRangeResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VersionResult resolveVersion( final RepositorySystemSession session, final VersionRequest request )
        throws VersionResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ArtifactDescriptorResult readArtifactDescriptor( final RepositorySystemSession session,
                                                            final ArtifactDescriptorRequest request )
        throws ArtifactDescriptorException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CollectResult collectDependencies( final RepositorySystemSession session, final CollectRequest request )
        throws DependencyCollectionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DependencyResult resolveDependencies( final RepositorySystemSession session, final DependencyRequest request )
        throws DependencyResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public List<ArtifactResult> resolveDependencies( final RepositorySystemSession session, final DependencyNode node,
                                                     final DependencyFilter filter )
        throws ArtifactResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    @Deprecated
    public List<ArtifactResult> resolveDependencies( final RepositorySystemSession session,
                                                     final CollectRequest request, final DependencyFilter filter )
        throws DependencyCollectionException, ArtifactResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ArtifactResult resolveArtifact( final RepositorySystemSession session, final ArtifactRequest request )
        throws ArtifactResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<ArtifactResult> resolveArtifacts( final RepositorySystemSession session,
                                                  final Collection<? extends ArtifactRequest> requests )
        throws ArtifactResolutionException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setMetadataFile( final File metadataFile )
    {
        this.metadataFile = metadataFile;
    }

    @Override
    public List<MetadataResult> resolveMetadata( final RepositorySystemSession session,
                                                 final Collection<? extends MetadataRequest> requests )
    {
        final List<MetadataResult> results = new ArrayList<MetadataResult>();
        for ( final MetadataRequest req : requests )
        {
            Metadata md = req.getMetadata();
            md = md.setFile( metadataFile );
            final MetadataResult result = new MetadataResult( req );
            result.setMetadata( md );
            results.add( result );
        }

        return results;
    }

    @Override
    public InstallResult install( final RepositorySystemSession session, final InstallRequest request )
        throws InstallationException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DeployResult deploy( final RepositorySystemSession session, final DeployRequest request )
        throws DeploymentException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LocalRepositoryManager newLocalRepositoryManager( final LocalRepository localRepository )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SyncContext newSyncContext( final RepositorySystemSession session, final boolean shared )
    {
        // TODO Auto-generated method stub
        return null;
    }
}
