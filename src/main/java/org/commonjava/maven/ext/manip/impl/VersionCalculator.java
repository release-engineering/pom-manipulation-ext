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

import static org.commonjava.maven.ext.manip.util.IdUtils.gav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.VersionCalculation;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.commonjava.maven.galley.maven.GalleyMavenException;
import org.commonjava.maven.galley.maven.model.view.MavenMetadataView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that calculates project version modifications, based on configuration stored in {@link VersioningState}. Snapshots may/may not be
 * preserved, and either a static or incremental (calculated) version qualifier may / may not be incorporated in the version. The calculator
 * strives for OSGi compatibility, so the use of '.' and '-' qualifier separators will vary accordingly.
 *
 * See: http://www.aqute.biz/Bnd/Versioning for an explanation of OSGi versioning.
 *
 * @author jdcasey
 */
@Component( role = VersionCalculator.class )
public class VersionCalculator
{

    private static final String SERIAL_SUFFIX_PATTERN = "(.+)([-.])(\\d+)$";

    public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

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
     * Calculate any project version changes for the given set of projects, and return them in a Map keyed by project GA.
     */
    public Map<String, String> calculateVersioningChanges( final Collection<Project> projects,
                                                           final ManipulationSession session )
        throws ManipulationException
    {
        final Map<String, VersionCalculation> calculationsByGA = new HashMap<String, VersionCalculation>();
        boolean incremental = false;
        for ( final Project project : projects )
        {
            final String originalVersion = project.getVersion();

            final VersionCalculation modifiedVersion =
                calculate( project.getGroupId(), project.getArtifactId(), originalVersion, session );

            if ( modifiedVersion.hasCalculation() )
            {
                incremental = incremental || modifiedVersion.isIncremental();
                calculationsByGA.put( gav( project ), modifiedVersion );
            }
        }

        if ( incremental )
        {
            int maxQualifier = 1;
            for ( final VersionCalculation calc : calculationsByGA.values() )
            {
                final int i = calc.getIncrementalQualifier();
                maxQualifier = maxQualifier > i ? maxQualifier : i;
            }

            for ( final VersionCalculation calc : calculationsByGA.values() )
            {
                calc.setIncrementalQualifier( maxQualifier );
            }
        }

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        for ( final Map.Entry<String, VersionCalculation> entry : calculationsByGA.entrySet() )
        {
            final String modifiedVersion = entry.getValue()
                                                .renderVersion();
            logger.debug( entry.getKey() + " has updated version: {}. Marking for rewrite.", modifiedVersion );
            versionsByGA.put( entry.getKey(), modifiedVersion );
        }

        return versionsByGA;
    }

    /**
     * Calculate the version modification for a given GAV.
     */
    // FIXME: Loooong method
    protected VersionCalculation calculate( final String groupId, final String artifactId,
                                            final String originalVersion, final ManipulationSession session )
        throws ManipulationException
    {
        String baseVersion = originalVersion;

        boolean snapshot = false;
        // If we're building a snapshot, make sure the resulting version ends
        // in "-SNAPSHOT"
        if ( baseVersion.endsWith( SNAPSHOT_SUFFIX ) )
        {
            snapshot = true;
            baseVersion = baseVersion.substring( 0, baseVersion.length() - SNAPSHOT_SUFFIX.length() );
        }

        final VersioningState state = session.getState( VersioningState.class );
        final String incrementalSuffix = state.getIncrementalSerialSuffix();
        final String staticSuffix = state.getSuffix();

        logger.debug( "Got the following version suffixes:\n  Static: " + staticSuffix + "\nIncremental: "
            + incrementalSuffix );

        final VersionCalculation vc = new VersionCalculation( originalVersion, baseVersion );
        if ( incrementalSuffix != null )
        {
                calculateIncremental( vc, baseVersion, snapshot, incrementalSuffix, groupId, artifactId,
                                      originalVersion, state, session );
        }
        else if ( staticSuffix != null )
        {
                calculateStatic( vc, baseVersion, snapshot, staticSuffix, groupId, artifactId, originalVersion, state,
                                 session );
        }

        // TODO OSGi fixup for versions like 1.2.GA or 1.2 (too few parts)

        // tack -SNAPSHOT back on if necessary...
        vc.setSnapshot( state.preserveSnapshot() && snapshot );

        return vc;
    }

    private void calculateStatic( final VersionCalculation vc, final String baseVersion,
                                                final boolean snapshot, final String suffix, final String groupId,
                                                final String artifactId, final String originalVersion,
                                                final VersioningState state, final ManipulationSession session )
    {
        final Pattern serialSuffixPattern = Pattern.compile( SERIAL_SUFFIX_PATTERN );
        final Matcher suffixMatcher = serialSuffixPattern.matcher( suffix );

        String suffixBase = suffix;
        String sep = "-";

        if ( suffixMatcher.matches() )
        {
            logger.debug( "Treating suffix {} as serial.", suffix );

            // the "redhat" in "redhat-1"
            suffixBase = suffixMatcher.group( 1 );
            sep = suffixMatcher.group( 2 );
            if ( sep == null )
            {
                sep = "-";
            }
        }

        trimBaseVersion( vc, baseVersion, suffixBase );

        vc.setVersionSuffix( suffix );
    }

    private String trimBaseVersion( final VersionCalculation vc, String baseVersion, final String suffixBase )
    {
        final int idx = baseVersion.indexOf( suffixBase );

        if ( idx > 1 )
        {
            // trim the old suffix off.
            final char baseSep = baseVersion.charAt( idx - 1 );
            baseVersion = baseVersion.substring( 0, idx - 1 );
            vc.setBaseVersionSeparator( Character.toString( baseSep ) );
            vc.setBaseVersion( baseVersion );
            logger.debug( "Trimmed version (without pre-existing suffix): " + baseVersion
                + " with base-version separator: " + baseSep );
        }

        if ( baseVersion.matches( ".+[-.]\\d+" ) )
        {
            vc.setBaseVersionSeparator( "." );
        }
        
        return baseVersion;
    }

    private void calculateIncremental( final VersionCalculation vc, String baseVersion,
                                                     final boolean snapshot, final String incrementalSerialSuffix,
                                                     final String groupId, final String artifactId,
                                                     final String originalVersion, final VersioningState state,
                                                     final ManipulationSession session )
        throws ManipulationException
    {
        logger.debug( "Using incremental suffix: " + incrementalSerialSuffix );

        String suffixBase = incrementalSerialSuffix;
        String sep = "-";

        final Matcher suffixMatcher = Pattern.compile( SERIAL_SUFFIX_PATTERN )
                                             .matcher( incrementalSerialSuffix );
        if ( suffixMatcher.matches() )
        {
            logger.debug( "Treating suffix {} as serial.", incrementalSerialSuffix );

            // the "redhat" in "redhat-1"
            suffixBase = suffixMatcher.group( 1 );
            sep = suffixMatcher.group( 2 );
            if ( sep == null )
            {
                sep = "-";
            }
        }

        baseVersion = trimBaseVersion( vc, baseVersion, suffixBase );

        logger.debug( "Resolving suffixes already found in metadata to determine increment base." );

        vc.setVersionSuffix( suffixBase );

        final List<String> versionCandidates = new ArrayList<String>();
        versionCandidates.add( originalVersion );
        versionCandidates.addAll( getMetadataVersions( groupId, artifactId, session ) );

        int maxSerial = 0;

        final String candidatePatternStr = suffixBase + sep + "(\\d+)$";
        logger.debug( "Using pattern: '{}' to find compatible versions from metadata.", candidatePatternStr );
        final Pattern candidateSuffixPattern = Pattern.compile( candidatePatternStr );
        for ( final String version : versionCandidates )
        {
            final Matcher candidateSuffixMatcher = candidateSuffixPattern.matcher( version );

            if ( candidateSuffixMatcher.find() )
            {
                final String wholeSuffix = candidateSuffixMatcher.group();
                logger.debug( "Group 0 of serial-suffix matcher is: '" + wholeSuffix + "'" );
                final int baseIdx = version.indexOf( wholeSuffix );

                // Need room for at least a character in the base-version, plus a separator like '-'
                if ( baseIdx < 2 )
                {
                    logger.debug( "Ignoring invalid version: '" + version
                        + "' (seems to be naked version suffix with no base)." );
                    continue;
                }

                final String base = version.substring( 0, baseIdx - 1 );
                logger.debug( "Candidate version base is: '{}'", base );
                if ( !baseVersion.equals( base ) )
                {
                    logger.debug( "Ignoring irrelevant version: '" + version + "' ('" + base
                        + "' doesn't match on base-version: '" + baseVersion + "')." );
                    continue;
                }

                // grab the old serial number.
                final String serialStr = candidateSuffixMatcher.group( 1 );
                logger.debug( "Group 1 of serial-suffix matcher is: '" + serialStr + "'" );
                final int serial = serialStr == null ? 0 : Integer.parseInt( serialStr );
                if ( serial > maxSerial )
                {
                    logger.debug( "new max serial number: " + serial + " (previous was: " + maxSerial + ")" );
                    maxSerial = serial;
                }
            }

            vc.setSuffixSeparator( sep );
            vc.setIncrementalQualifier( maxSerial + 1 );
        }
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
