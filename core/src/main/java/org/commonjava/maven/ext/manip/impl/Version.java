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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Component that reads a version string and makes various modifications to it such as converting to a valid OSGi
 * version string and/or incrementing a version suffix. See: http://www.aqute.biz/Bnd/Versioning for an explanation of
 * OSGi versioning. Parses versions into the following format: &lt;major&gt;.&lt;minor&gt;.&lt;micro&gt;
 * .&lt;qualifierBase&gt;-&lt;buildnumber&gt;-&lt;buildnumber&gt;-&lt;snapshot&gt;
 */
public class Version
{
    private static final Logger logger = LoggerFactory.getLogger( Version.class );

    private final static Character[] DEFAULT_DELIMITERS = { '.', '-', '_' };

    private final static char OSGI_VERSION_DELIMITER = '.';

    /**
     * Regular expression used to match version string delimiters
     */
    private final static String DELIMITER_REGEX = "[\\.\\-_]";

    /**
     * Regular expression used to match the major, minor, and micro versions
     */
    private final static String MMM_REGEX = "(\\d+)(" + DELIMITER_REGEX + "(\\d+)?(" + DELIMITER_REGEX + "(\\d+))?)?";

    private final static Pattern mmmPattern = Pattern.compile( MMM_REGEX );

    private final static String SNAPSHOT_SUFFIX = "SNAPSHOT";

    /**
     * Regular expression used to match the parts of the qualifier "base-buildnum-snapshot"
     */
    private final static String QUALIFIER_REGEX = "(.*?)((" + DELIMITER_REGEX + ")?(\\d+))?$";

    private final static Pattern qualifierPattern = Pattern.compile( QUALIFIER_REGEX );

    private final static String VERSION_REGEX = "(" + MMM_REGEX + ")?" + "(" + DELIMITER_REGEX + ")?"
            + "(" + QUALIFIER_REGEX + ")";

    private final static Pattern versionPattern = Pattern.compile( VERSION_REGEX );

    private final static String SNAPSHOT_REGEX = "(.*?)(" + DELIMITER_REGEX + ")?((?i:" + SNAPSHOT_SUFFIX + "))?$";

    private final static Pattern snapshotPattern = Pattern.compile( SNAPSHOT_REGEX );

    /**
     * Used to match valid OSGi version
     */
    private final static String OSGI_VERSION_REGEX = "(\\d+)(\\.\\d+(\\.\\d+([\\.][\\p{Alnum}|\\-|_]+)?)?)?";

    private final static Pattern osgiPattern = Pattern.compile( OSGI_VERSION_REGEX );

    private final List<Character> versionStringDelimiters = Arrays.asList( DEFAULT_DELIMITERS );

    /**
     * The original version string before any modifications
     */
    private final String originalVersion;

    /**
     * The original unmodified major, minor, micro portion of the version string.
     */
    private String originalMMM;

    private String majorVersion;

    private String minorVersion;

    private String microVersion;

    /**
     * The original unmodified version qualifier. Will be null if no qualifier is included.
     */
    private String originalQualifier;

    /**
     * The delimiter between the MMM version and the qualifier
     */
    private static final char DEFAULT_MMM_DELIMITER = OSGI_VERSION_DELIMITER;

    /**
     * The delimiter between the parts of the qualifier
     */
    private static final char DEFAULT_QUALIFIER_DELIMITER = '-';

    /**
     * The original delimiter between the MMM version and the qualifier.
     */
    private String qualifierStartDelimiter;

    private String qualifierBase;

    private String buildNumberDelimiter = Character.toString( DEFAULT_QUALIFIER_DELIMITER );

    /**
     * Numeric string at the end of the qualifier
     */
    private String buildNumber;

    private String snapshotDelimiter;

    /**
     * The snapshot portion of the qualifier
     */
    private String snapshot;

    /**
     * Represents whether the major, minor, micro versions are valid integers. This will be false if the version string
     * uses a property string like "${myVersion}-build-1" or if the version string starts with alpha chars like
     * "GA-1-Beta". In these cases we can't parse the major, minor, micro versions, so we just leave the string intact.
     */
    private boolean numericVersion = true;

    public Version( String version )
    {
        originalVersion = version;
        logger.debug( "Parsing version: " + originalVersion );
        parseVersion( originalVersion );
        logger.debug( "Major: " + getMajorVersion() + ", Minor: " + getMinorVersion() + ",  Micro: " +
            getMicroVersion() );
        logger.debug( "Qualifier: " + getQualifier() + ", Base: " + getQualifierBase() + ", BuildNum: " +
            getBuildNumber() );
    }

    /**
     * Parse a version string into its component parts (major, minor, micro, qualifier). By default will split the
     * String based on ".", "-", and "_".
     *
     * @param version
     * @return
     */
    private void parseVersion( final String version )
    {
        Matcher snapshotMatcher = snapshotPattern.matcher( version );
        snapshotMatcher.matches();
        final String versionWithoutSnapshot = snapshotMatcher.group( 1 );
        snapshotDelimiter = snapshotMatcher.group( 2 );
        snapshot = snapshotMatcher.group( 3 );

        Matcher versionMatcher = versionPattern.matcher( versionWithoutSnapshot );
        if ( !versionMatcher.matches() )
        {
            return;
        }
        originalMMM = versionMatcher.group( 1 );
        qualifierStartDelimiter = versionMatcher.group( 7 );
        originalQualifier = versionMatcher.group( 8 );

        parseMMM( originalMMM );
        parseQualifier( originalQualifier );
    }

    /**
     * Parse the mmm (major, minor, micro) portions of the version string
     *
     * @param mmm The numeric portion of the version string before the qualifier
     */
    private void parseMMM( String mmm )
    {
        if ( isEmpty( mmm ) )
        {
            numericVersion = false;
            return;
        }

        Matcher mmmMatcher = mmmPattern.matcher( mmm );
        mmmMatcher.matches();

        majorVersion = mmmMatcher.group( 1 );
        String minor = mmmMatcher.group( 3 );
        if ( !isEmpty( minor ) )
        {
            minorVersion = minor;
        }
        String micro = mmmMatcher.group( 5 );
        if ( !isEmpty( micro ) )
        {
            microVersion = micro;
        }
    }

    /**
     * Parses the qualifier into the format &gt;qualifierBase&lt;-&gt;buildnumber&lt;-&gt;snapshot&lt;.
     *
     * @param qualifier
     */
    private void parseQualifier( String qualifier )
    {
        if ( isEmpty( qualifier ) )
        {
            return;
        }

        Matcher qualifierMatcher = qualifierPattern.matcher( qualifier );
        qualifierMatcher.matches();

        qualifierBase = qualifierMatcher.group( 1 );
        buildNumberDelimiter = qualifierMatcher.group( 3 );
        if ( buildNumberDelimiter == null )
        {
            buildNumberDelimiter = "";
        }
        buildNumber = qualifierMatcher.group( 4 );
        if ( buildNumber == null )
        {
            buildNumberDelimiter = Character.toString( DEFAULT_QUALIFIER_DELIMITER );
        }
    }

    /**
     * Remove any delimiters from the end of the string
     *
     * @param partialVersionString
     * @return
     */
    private String removeLastDelimiters( String partialVersionString )
    {
        while ( !isEmpty( partialVersionString ) &&
            versionStringDelimiters.contains( partialVersionString.charAt( partialVersionString.length() - 1 ) ) )
        {
            partialVersionString = partialVersionString.substring( 0, partialVersionString.length() - 1 );
        }
        return partialVersionString;
    }

    private static boolean isEmpty( String string )
    {
        if ( string == null )
        {
            return true;
        }
        if ( string.trim().equals( "" ) )
        {
            return true;
        }
        return false;
    }

    /**
     * Check if all the characters in the string are digits
     *
     * @param str a string to check
     * @return whether all the characters in the string are digits
     */
    public static boolean isNumeric( String str )
    {
        for ( char c : str.toCharArray() )
        {
            if ( !Character.isDigit( c ) )
                return false;
        }
        return true;
    }

    /**
     * Checks if the original version string is a valid OSGi version.
     *
     * @return true if the version is valid
     */
    public boolean isValidOSGi()
    {
        Matcher osgiMatcher = osgiPattern.matcher( originalVersion );
        return osgiMatcher.matches();
    }

    public String getMajorVersion()
    {
        if ( isEmpty( majorVersion) )
        {
            majorVersion = "0";
        }
        return majorVersion;
    }

    /**
     * Assumed to be "0" if no minor version is specified in the string
     *
     * @return the minor version
     */
    public String getMinorVersion()
    {
        if ( isEmpty( minorVersion ) )
        {
            minorVersion = "0";
        }
        return minorVersion;
    }

    /**
     * Assumed to be "0" if no micro version is specified in the string
     *
     * @return the micro version
     */
    public String getMicroVersion()
    {
        if ( isEmpty( microVersion ) )
        {
            microVersion = "0";
        }
        return microVersion;
    }

    /**
     * Get the original version string that was used to create this version object.
     *
     * @return the original version string
     */
    public String getOriginalVersion()
    {
        return originalVersion;
    }

    /**
     * Get original unmodified major, minor, micro portion of the version string
     *
     * @return the original major/minor/micro version.
     */
    public String getOriginalMMM()
    {
        if ( originalMMM == null )
        {
            originalMMM = "";
        }
        return originalMMM;
    }

    /**
     * Generate the qualifier by combining the qualifierBase, qualifierSuffix, and build number
     *
     * @return The qualifier part of the version string (an empty string if there is no qualifier)
     */
    public String getQualifier()
    {
        StringBuilder qualifier = new StringBuilder();
        qualifier.append( getQualifierBaseAndBuildNum() );

        if ( isSnapshot() )
        {
            if ( qualifier.length() > 0 )
            {
                qualifier.append( DEFAULT_QUALIFIER_DELIMITER );
            }
            qualifier.append( this.snapshot );
        }

        return qualifier.toString();
    }

    public String getQualifierBase()
    {
        if ( qualifierBase == null )
        {
            qualifierBase = "";
        }
        return qualifierBase;
    }

    public String getQualifierBaseAndBuildNum()
    {
        StringBuilder qualBaseAndNum = new StringBuilder( getQualifierBase() );
        if ( !isEmpty( getBuildNumber() ) )
        {
            if ( qualBaseAndNum.length() > 0 )
            {
                qualBaseAndNum.append( getBuildNumberDelimiter() );
            }
            qualBaseAndNum.append( getBuildNumber() );
        }
        return qualBaseAndNum.toString();
    }

    public String getVersionString()
    {
        if ( isEmpty( getQualifier() ) )
        {
            return originalVersion;
        }
        if ( isEmpty( originalMMM ) )
        {
            return getQualifier();
        }

        StringBuilder versionString = new StringBuilder();
        versionString.append( originalMMM );
        if ( isEmpty( qualifierStartDelimiter ) )
        {
            versionString.append( DEFAULT_MMM_DELIMITER );
        }
        else
        {
            versionString.append( qualifierStartDelimiter  );
        }
        versionString.append( getQualifier() );
        return versionString.toString();
    }

    public String getOSGiVersionString()
    {
        if ( isValidOSGi() && !hasQualifier() )
        {
            return originalVersion;
        }
        StringBuilder osgiVersion = new StringBuilder();
        if ( numericVersion )
        {
            osgiVersion.append( getThreePartMMM() );
        }
        if ( !isEmpty( getQualifier() ) )
        {
            if ( numericVersion )
            {
                osgiVersion.append( OSGI_VERSION_DELIMITER );
            }
            osgiVersion.append( getOSGiQualifier() );
        }
        return osgiVersion.toString();

    }

    /**
     * Get the major, minor, micro version string and use zeros if the minor or micro were not set in the original
     * version
     *
     * @return
     */
    private String getThreePartMMM()
    {
        StringBuilder mmm = new StringBuilder();
        mmm.append( getMajorVersion() );
        mmm.append( OSGI_VERSION_DELIMITER );
        mmm.append( getMinorVersion() );
        mmm.append( OSGI_VERSION_DELIMITER );
        mmm.append( getMicroVersion() );
        return mmm.toString();
    }

    /**
     * Replaces "." delimiters with a "-";
     *
     * @return
     */
    private String getOSGiQualifier()
    {
        if ( getQualifier() == null )
        {
            return null;
        }
        return getQualifier().replace( '.', '-' );
    }

    public boolean isSnapshot()
    {
        return SNAPSHOT_SUFFIX.equalsIgnoreCase( this.snapshot );
    }

    public String getBuildNumber()
    {
        return buildNumber;
    }

    public String getBuildNumberDelimiter()
    {
        if ( buildNumberDelimiter == null )
        {
            buildNumberDelimiter = "";
        }
        return buildNumberDelimiter;
    }

    public boolean hasBuildNumber()
    {
        return !isEmpty( getBuildNumber() );
    }

    public boolean hasQualifier()
    {
        return !isEmpty( getQualifier() );
    }

    /**
     * Appends the given qualifier suffix.  Attempts to match the given qualifier suffix to the current suffix
     * to avoid duplicates like "1.0-beta-beta.  If the suffix matches the existing one, does nothing.
     *
     * @param suffix The qualifier suffix to append. This can be a simple string like "foo", or it can optionally
     *            include a build number, for example "foo-1", which will automatically be set as the build number for
     *            this version.
     */
    public void appendQualifierSuffix( String suffix )
    {
        logger.debug( "Applying suffix: " + suffix + " to version " + getVersionString() );
        if ( suffix == null )
        {
            return;
        }

        // Remove snapshot portion first to make parsing simpler
        Matcher snapshotMatcher = snapshotPattern.matcher( suffix );
        snapshotMatcher.matches();
        final String suffixWithoutSnapshot = snapshotMatcher.group( 1 );
        final String suffixSnapshotDelimiter = snapshotMatcher.group( 2 );
        final String suffixSnapshot = snapshotMatcher.group( 3 );
        if ( !isEmpty( suffixSnapshot ) )
        {
            this.snapshotDelimiter = suffixSnapshotDelimiter;
            this.snapshot = suffixSnapshot;
        }

        // Handle the non-snapshot portion of the new suffix
        Matcher suffixMatcher = qualifierPattern.matcher( suffixWithoutSnapshot );
        if ( !suffixMatcher.matches() )
        {
            return;
        }

        String suffixBase = suffixMatcher.group( 1 );
        String suffixBuildNumberDelimiter = suffixMatcher.group( 3 );
        String suffixBuildNumber = suffixMatcher.group( 4 );

        String suffixBaseNoDelim = this.removeLastDelimiters( suffixBase );
        String suffixMatchRegex = "(.*?)(" + Pattern.quote( suffixBaseNoDelim ) + ")(" + DELIMITER_REGEX + "?)";

        String oldQualifierBase = getQualifierBase();
        String oldQualifierBaseAndNum = getQualifierBaseAndBuildNum();
        if ( isEmpty( oldQualifierBaseAndNum ) )
        {
            if ( suffixBase.length() > 0 && versionStringDelimiters.contains( suffixBase.charAt( 0 ) ) )
            {
                qualifierStartDelimiter = Character.toString( suffixBase.charAt( 0 ) );
                suffixBase = suffixBase.substring( 1 );
            }
            qualifierBase = suffixBase;
        }
        // Check if the new suffix matches the end of the existing qualifier
        else if ( !Pattern.matches( suffixMatchRegex, oldQualifierBase ) )
        {
            // If the suffix doesn't match, and there is an existing build number
            // the old build number becomes part of the qualifier base
            // e.g. "1.2.0.Beta-1" + "foo-2" = "1.2.0.Beta-1-foo-2"
            StringBuilder newQualifierBase = new StringBuilder( oldQualifierBaseAndNum );
            buildNumber = null;

            if ( newQualifierBase.length() > 0 &&
                    !versionStringDelimiters.contains( newQualifierBase.charAt( newQualifierBase.length() - 1 ) ) )
            {
                newQualifierBase.append( DEFAULT_QUALIFIER_DELIMITER );
            }
            newQualifierBase.append(suffixBase);
            qualifierBase = newQualifierBase.toString();
        }

        if ( !isEmpty( suffixBuildNumber ) )
        {
            buildNumberDelimiter = suffixBuildNumberDelimiter;
            buildNumber = suffixBuildNumber;
        }

        logger.debug( "New version string: " + getVersionString() );
    }

    /**
     * Appends a build number to the qualifier. The build number should be a string of digits, or null if the build
     * number should be removed.
     *
     * @param buildNumber to append to the qualifier.
     */
    public void setBuildNumber( String buildNumber )
    {
        if ( buildNumber == null || isNumeric( buildNumber ) )
        {
            this.buildNumber = buildNumber;
        }
    }

    /**
     * Sets the snapshot to "SNAPSHOT" if true, otherwise sets to null
     *
     * @param snapshot whether to append SNAPSHOT or not.
     */
    public void setSnapshot( boolean snapshot )
    {
        if ( snapshot )
        {
            this.snapshot = SNAPSHOT_SUFFIX;
        }
        else
        {
            this.snapshot = null;
        }
    }

    /**
     * Matches a version object to versions in a set by comparing the non build number portion of the string. Then find
     * which of the matching versions has the highest build number and is therefore the latest version.
     *
     * @param version the Version object to use.
     * @param versionSet a collection of versions to compare to.
     * @return the highest build number, or 0 if no matching build numbers are found.
     */
    public static int findHighestMatchingBuildNumber( Version version, Set<String> versionSet )
    {
        int highestBuildNum = 0;

        // Build version pattern regex, matches something like "<mmm>.<qualifier>.<buildnum>".
        StringBuilder versionPatternBuf = new StringBuilder();
        versionPatternBuf.append( '(' )
                .append( Pattern.quote( version.getOriginalMMM() ) ).append('(').append( DELIMITER_REGEX ).append("0)*") // Match zeros appended to a major only version
                .append( ")?" )
                .append( DELIMITER_REGEX );
        if ( !isEmpty( version.getQualifierBase() ) )
        {
            versionPatternBuf.append( Pattern.quote( version.getQualifierBase() ) );
            versionPatternBuf.append( DELIMITER_REGEX );
        }
        versionPatternBuf.append( "(\\d+)" );
        String candidatePatternStr = versionPatternBuf.toString();

        logger.debug( "Using pattern: '{}' to find compatible versions from metadata.", candidatePatternStr );
        final Pattern candidateSuffixPattern = Pattern.compile( candidatePatternStr );

        for ( final String compareVersion : versionSet )
        {
            final Matcher candidateSuffixMatcher = candidateSuffixPattern.matcher( compareVersion );
            if ( candidateSuffixMatcher.matches() )
            {
                String buildNumberStr = candidateSuffixMatcher.group( 3 );
                int compareBuildNum = Integer.parseInt( buildNumberStr );
                if ( compareBuildNum > highestBuildNum )
                {
                    highestBuildNum = compareBuildNum;
                }
            }
        }
        logger.debug ("Found highest matching build number {} from set {} ", highestBuildNum, versionSet);

        return highestBuildNum;
    }

    /**
     * Get the build number as an integer instead of a string for each numeric comparison
     *
     * @return the build number as an integer.
     */
    public int getIntegerBuildNumber()
    {
        if ( this.isEmpty( buildNumber ) )
        {
            return 0;
        }
        return Integer.parseInt( buildNumber );
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "Version: " );
        buffer.append( getVersionString() );
        buffer.append( ", OSGi Version: " );
        buffer.append( getOSGiVersionString() );
        return buffer.toString();
    }
}
