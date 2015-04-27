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
package org.commonjava.maven.ext.manip.impl;

import static org.commonjava.maven.ext.manip.util.IdUtils.gav;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.meta.MavenMetadataView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that calculates project version modifications, based on configuration stored in {@link VersioningState}.
 * Snapshots may/may not be preserved, and either a static or incremental (calculated) version qualifier may / may not
 * be incorporated in the version. The calculator strives for OSGi compatibility, so the use of '.' and '-' qualifier
 * separators will vary accordingly. See: http://www.aqute.biz/Bnd/Versioning for an explanation of OSGi versioning.
 *
 * @author jdcasey
 */
@Component( role = VersionCalculator.class )
public class VersionCalculator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected GalleyAPIWrapper readerWrapper;

    protected VersionCalculator()
    {
    }

    public VersionCalculator( final GalleyAPIWrapper readerWrapper )
    {
        this.readerWrapper = readerWrapper;
    }

    /**
     * Calculate any project version changes for the given set of projects, and return them in a Map keyed by project
     * GA.
     *
     * @param projects
     * @param session
     * @return Map<String, String>
     * @throws ManipulationException
     */
    public Map<String, String> calculateVersioningChanges( final Collection<Project> projects,
                                                           final ManipulationSession session )
        throws ManipulationException
    {
        final VersioningState state = session.getState( VersioningState.class );
        final Map<String, String> versionsByGA = new HashMap<String, String>();
        final Map<String, Version> versionObjsByGA = new HashMap<String, Version>();
        final Set<String> versionSet = new HashSet<String>();

        for ( final Project project : projects )
        {
            final String originalVersion = project.getVersion();
            String modifiedVersionString;

            final Version modifiedVersion =
                calculate( project.getGroupId(), project.getArtifactId(), originalVersion, session );
            versionObjsByGA.put( gav( project ), modifiedVersion );

            if ( state.osgi() )
            {
                modifiedVersionString = modifiedVersion.getOSGiVersionString();
            }
            else
            {
                modifiedVersionString = modifiedVersion.getVersionString();
            }

            if ( modifiedVersion.hasBuildNumber() )
            {
                versionSet.add( modifiedVersionString );
            }
        }

        // Have to loop through the versions a second time to make sure that the versions are in sync
        // between projects in the reactor.
        for ( final Project project : projects )
        {
            final String originalVersion = project.getVersion();
            String modifiedVersionString;

            final Version modifiedVersion = versionObjsByGA.get( gav( project ) );

            int buildNumber = modifiedVersion.findHighestMatchingBuildNumber( modifiedVersion, versionSet );

            // If the buildNumber is greater than zero, it means we found a match and have to
            // set the build number to avoid version conflicts.
            if ( buildNumber > 0 )
            {
                modifiedVersion.setBuildNumber( Integer.toString( buildNumber ) );
            }

            if ( state.osgi() )
            {
                modifiedVersionString = modifiedVersion.getOSGiVersionString();
            }
            else
            {
                modifiedVersionString = modifiedVersion.getVersionString();
            }

            versionSet.add( modifiedVersionString );
            logger.debug( gav( project ) + " has updated version: {}. Marking for rewrite.", modifiedVersionString );

            if ( !originalVersion.equals( modifiedVersionString ) )
            {
                versionsByGA.put( gav( project ), modifiedVersionString );
            }

        }

        return versionsByGA;
    }

    /**
     * Calculate the version modification for a given GAV.
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @param session
     * @return VersionCalculation
     * @throws ManipulationException
     */
    protected Version calculate( final String groupId, final String artifactId, final String version,
                                 final ManipulationSession session )
        throws ManipulationException
    {
        final VersioningState state = session.getState( VersioningState.class );

        final String incrementalSuffix = state.getIncrementalSerialSuffix();
        final String staticSuffix = state.getSuffix();
        final String override = state.getOverride();

        logger.debug( "Got the following version:\n  Original version: " + version );
        logger.debug( "Got the following version suffixes:\n  Static: " + staticSuffix + "\n  Incremental: " +
            incrementalSuffix );
        logger.debug( "Got the following override:\n  Version: " + override);

        Version versionObj;

        if ( override != null )
        {
            versionObj = new Version( override );
        }
        else
        {
            versionObj = new Version( version );
        }

        if ( staticSuffix != null )
        {
            versionObj.appendQualifierSuffix( staticSuffix );
            if ( !state.preserveSnapshot() )
            {
                versionObj.setSnapshot( false );
            }
        }
        else if ( incrementalSuffix != null )
        {
            // Find matching version strings in the remote repo and increment to the next
            // available version
            final Set<String> versionCandidates = new HashSet<String>();
            versionCandidates.addAll( getMetadataVersions( groupId, artifactId, session ) );
            versionObj.appendQualifierSuffix( incrementalSuffix );
            int highestRemoteBuildNum = versionObj.findHighestMatchingBuildNumber( versionObj, versionCandidates );
            ++highestRemoteBuildNum;
            if ( highestRemoteBuildNum > versionObj.getIntegerBuildNumber() )
            {
                versionObj.setBuildNumber( Integer.toString( highestRemoteBuildNum ) );
            }
            if ( !state.preserveSnapshot() )
            {
                versionObj.setSnapshot( false );
            }
        }

        return versionObj;
    }

    /**
     * Accumulate all available versions for a given GAV from all available repositories.
     */
    private Set<String> getMetadataVersions( final String groupId, final String artifactId,
                                             final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Reading available versions from repository metadata for: " + groupId + ":" + artifactId );

        try
        {
            final MavenMetadataView metadataView =
                readerWrapper.readMetadataView( new ProjectRef( groupId, artifactId ) );

            final List<String> versions =
                metadataView.resolveXPathToAggregatedStringList( "/metadata/versioning/versions/version", true, -1 );

            return new HashSet<String>( versions );
        }
        catch ( final GalleyMavenException e )
        {
            throw new ManipulationException( "Failed to resolve metadata for: %s:%s.", e, groupId, artifactId );
        }
    }
}
