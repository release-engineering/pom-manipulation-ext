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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that reads a version string and makes various modifications to it such as converting to a valid OSGi
 * version string and/or incrementing a version suffix. See: http://www.aqute.biz/Bnd/Versioning for an explanation of
 * OSGi versioning. Parses versions into the following format: &lt;major&gt;.&lt;minor&gt;.&lt;micro&gt;
 * .&lt;qualifierBase&gt;-&lt;buildnumber&gt;-&lt;buildnumber&gt;-&lt;snapshot&gt;
 */
public class Version
{

    private final Character[] DEFAULT_DELIMITERS = { '.', '-', '_' };

    private final static String DELIMITER_REGEX = "[\\.\\-_]?";

    private final char OSGI_VERSION_DELIMITER = '.';

    // Used to match valid osgi version
    private static final String OSGI_VERSION_REGEX = "(\\d+)(\\.\\d+(\\.\\d+([\\.][\\p{Alnum}|\\-|_]+)?)?)?";

    private final String SNAPSHOT_SUFFIX = "SNAPSHOT";

    private List<Character> versionStringDelimiters = Arrays.asList( DEFAULT_DELIMITERS );

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
     * The current qualifier, after any modifications such as suffix or build number changes have been made.
     */
    private String qualifier;

    private String qualifierBase;

    /**
     * Numeric string at the end of the qualifier
     */
    private String buildNumber;

    private String snapshot;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Represents whether the major, minor, micro versions are valid integers. This will be false if the version string
     * uses a property string like "${myVersion}-build-1" or if the version string starts with alpha chars like
     * "GA-1-Beta". In these cases we can't parse the major, minor, micro versions, so we just leave the string intact.
     */
    private boolean numericVersion = true;

    public Version( String version )
    {
        originalVersion = version;
        parseVersion( originalVersion );
        logger.debug( "Parsed version: " + version );
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
    private final void parseVersion( String version )
    {
        int qualifierIndex = getQualifierIndex( version );
        originalMMM = version.substring( 0, qualifierIndex );
        originalQualifier = version.substring( qualifierIndex );
        qualifier = originalQualifier;

        parseMMM( originalMMM );
        parseQualifier( originalQualifier );
    }

    /**
     * Attempt to find where the qualifier portion of the version string begins. Will return the location of either the
     * first non-numeric char or the character after the third delimiter.
     * 
     * @param version
     * @return index of the start of the qualifier (version.length() if no qualifier was found)
     */
    private int getQualifierIndex( String version )
    {
        final int QUALIFIER_NOT_FOUND = version.length();

        int qualifierIndex = 0;
        int delimiterCount = 0;
        while ( qualifierIndex < version.length() )
        {
            if ( versionStringDelimiters.contains( version.charAt( qualifierIndex ) ) )
            {
                ++delimiterCount;
            }
            else if ( !isNumeric( Character.toString( version.charAt( qualifierIndex ) ) ) )
            {
                return qualifierIndex;
            }

            ++qualifierIndex;

            if ( delimiterCount == 3 )
            {
                return qualifierIndex;
            }
        }
        return QUALIFIER_NOT_FOUND;

    }

    /**
     * Parse the mmm (major, minor, micro) portions of the version string
     * 
     * @param mmm The numeric portion of the version string before the qualifier
     */
    private void parseMMM( String mmm )
    {
        // Default to "0" for any missing versions
        majorVersion = "0";
        minorVersion = "0";
        microVersion = "0";

        if ( isEmpty( mmm ) )
        {
            numericVersion = false;
            return;
        }

        String remainingMMMString = mmm;

        // MAJOR VERSION
        // If the first character is a delimiter, then we put a zero as the major version
        if ( versionStringDelimiters.contains( remainingMMMString.charAt( 0 ) ) )
        {
            majorVersion = "0";
        }
        else if ( !isNumeric( Character.toString( remainingMMMString.charAt( 0 ) ) ) )
        {
            // Invalid mmm, just return
            logger.warn( "Trying to parse an invalid major, minor, micro string: " + mmm );
            return;
        }
        else
        {
            majorVersion = getNextVersionPart( remainingMMMString );
            remainingMMMString = remainingMMMString.substring( majorVersion.length() );
        }

        // MINOR VERSION
        remainingMMMString = removeNextDelimiters( remainingMMMString );
        if ( !isEmpty( remainingMMMString ) )
        {
            minorVersion = getNextVersionPart( remainingMMMString );
            remainingMMMString = remainingMMMString.substring( minorVersion.length() );
        }

        // MICRO VERSION
        remainingMMMString = removeNextDelimiters( remainingMMMString );
        if ( !isEmpty( remainingMMMString ) )
        {
            microVersion = getNextVersionPart( remainingMMMString );
            remainingMMMString = remainingMMMString.substring( microVersion.length() );
        }
    }

    /**
     * Parses the qualifier into the following format.
     * 
     * @param qualifier
     */
    private void parseQualifier( String qualifier )
    {
        if ( isEmpty( qualifier ) )
        {
            return;
        }

        String remainingQualString = qualifier;
        List<String> qualifierParts = new ArrayList<String>();

        // Break the qualifier into sections based on the delimiters
        // We have to do this manually because regex can't search backwards
        while ( !isEmpty( remainingQualString ) )
        {
            remainingQualString = removeNextDelimiters( remainingQualString );
            String nextVersionPart = getNextVersionPart( remainingQualString );
            qualifierParts.add( nextVersionPart );
            remainingQualString = remainingQualString.substring( ( nextVersionPart.length() ) );
        }

        remainingQualString = qualifier;
        remainingQualString = removeLastDelimiters( remainingQualString );

        // Check if it's a snapshot
        String lastQualifierPart = qualifierParts.remove( qualifierParts.size() - 1 );
        if ( SNAPSHOT_SUFFIX.equalsIgnoreCase( lastQualifierPart ) )
        {
            this.snapshot = lastQualifierPart;
            remainingQualString = remainingQualString.substring( 0, remainingQualString.length() - snapshot.length() );
            remainingQualString = removeLastDelimiters( remainingQualString );
            if ( qualifierParts.isEmpty() )
            {
                return;
            }
            else
            {
                lastQualifierPart = qualifierParts.remove( qualifierParts.size() - 1 );
            }
        }

        // Try to extract the build number
        if ( isNumeric( lastQualifierPart ) )
        {
            buildNumber = lastQualifierPart;
            remainingQualString =
                remainingQualString.substring( 0, remainingQualString.length() - buildNumber.length() );
            remainingQualString = removeLastDelimiters( remainingQualString );
            if ( qualifierParts.isEmpty() )
            {
                return;
            }
            else
            {
                lastQualifierPart = qualifierParts.remove( qualifierParts.size() - 1 );
            }
        }

        this.qualifierBase = remainingQualString;
    }

    /**
     * @param remainingVersionString
     * @return
     */
    private String getNextVersionPart( String remainingVersionString )
    {

        // If we only have one or zeros characters left, the parsing is done, so just return
        if ( remainingVersionString.length() <= 1 )
        {
            return remainingVersionString;
        }

        // Find out if we're looking at a number or an alpha version part
        boolean isNumeric = false;
        if ( Character.isDigit( remainingVersionString.charAt( 0 ) ) )
        {
            isNumeric = true;
        }

        int index = 0;

        StringBuilder versionPart = new StringBuilder();
        while ( index < remainingVersionString.length() )
        {
            char nextChar = remainingVersionString.charAt( index );
            // Check if the next char matches what we're looking for
            if ( versionStringDelimiters.contains( nextChar ) )
            {
                break;
            }
            else if ( isNumeric && !Character.isDigit( nextChar ) )
            {
                break;
            }
            else if ( !isNumeric && Character.isDigit( nextChar ) )
            {
                break;
            }
            versionPart.append( remainingVersionString.charAt( index ) );
            ++index;
        }
        return versionPart.toString();
    }

    /**
     * Remove any delimiters from the beginning of the string
     * 
     * @param partialVersionString
     * @return
     */
    private String removeNextDelimiters( String partialVersionString )
    {
        while ( !isEmpty( partialVersionString ) && versionStringDelimiters.contains( partialVersionString.charAt( 0 ) ) )
        {
            partialVersionString = partialVersionString.substring( 1 );
        }
        return partialVersionString;
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

    private boolean isEmpty( String string )
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
     * @param str
     * @return
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
        return Pattern.matches( OSGI_VERSION_REGEX, originalVersion );
    }

    public String getMajorVersion()
    {
        return majorVersion;
    }

    /**
     * Assumed to be "0" if no minor version is specified in the string
     * 
     * @return
     */
    public String getMinorVersion()
    {
        return minorVersion;
    }

    /**
     * Assumed to be "0" if no micro version is specified in the string
     * 
     * @return
     */
    public String getMicroVersion()
    {
        return microVersion;
    }

    /**
     * Update the qualifier by combining the qualifierBase, qualifierSuffix, and build number
     * 
     * @return
     */
    private void updateQualifier()
    {

        StringBuilder updatedQualifier = new StringBuilder();

        if ( !isEmpty( getQualifierBase() ) )
        {
            updatedQualifier.append( getQualifierBase() );
            if ( !isEmpty( getBuildNumber() ) || isSnapshot() )
            {
                updatedQualifier.append( '-' );
            }
        }

        if ( !isEmpty( getBuildNumber() ) )
        {
            updatedQualifier.append( getBuildNumber() );
            if ( isSnapshot() )
            {
                updatedQualifier.append( '-' );
            }
        }

        if ( isSnapshot() )
        {
            updatedQualifier.append( this.snapshot );
        }

        qualifier = updatedQualifier.toString();
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
     * @return
     */
    public String getOriginalMMM()
    {
        return originalMMM;
    }

    /**
     * Generate the qualifier by combining the qualifierBase, qualifierSuffix, and build number
     * 
     * @return The qualifier part of the version string (an empty string if there is no qualifier)
     */
    public String getQualifier()
    {
        return qualifier;
    }

    public String getQualifierBase()
    {
        return qualifierBase;
    }

    public String getVersionString()
    {
        return originalMMM + getQualifier();
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
            osgiVersion.append( getMajorVersion() );

            if ( !isEmpty( getMinorVersion() ) )
            {
                osgiVersion.append( OSGI_VERSION_DELIMITER );
                osgiVersion.append( getMinorVersion() );
            }
            if ( !isEmpty( getMicroVersion() ) )
            {
                osgiVersion.append( OSGI_VERSION_DELIMITER );
                osgiVersion.append( getMicroVersion() );
            }
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

    public boolean hasBuildNumber()
    {
        return !isEmpty( getBuildNumber() );
    }

    public boolean hasQualifier()
    {
        return !isEmpty( getQualifier() );
    }

    /**
     * Sets the qualifier suffix to the current version. If the suffix matches the existing one, does nothing
     * 
     * @param partialSuffix The qualifier suffix to append. This can be a simple string like "foo", or it can optionally
     *            include a build number, for example "foo-1", which will automatically be set as the build number for
     *            this version.
     */
    public void appendQualifierSuffix( String suffix )
    {
        if ( suffix == null )
        {
            return;
        }

        String partialSuffix = suffix;
        partialSuffix = removeNextDelimiters( partialSuffix );

        // Check if the new suffix includes a buld number
        StringBuilder newBuildNumber = new StringBuilder();
        while ( isNumeric( partialSuffix.substring( partialSuffix.length() - 1 ) ) )
        {
            newBuildNumber.insert( 0, partialSuffix.substring( partialSuffix.length() - 1 ) );
            partialSuffix = partialSuffix.substring( 0, partialSuffix.length() - 1 );
        }

        partialSuffix = this.removeLastDelimiters( partialSuffix );

        String qualifierPatternStr = ".*" + partialSuffix;

        if ( isEmpty( qualifierBase ) || !Pattern.matches( qualifierPatternStr, qualifierBase ) )
        {
            String oldQualifier = qualifier;
            if ( isSnapshot() )
            {
                oldQualifier = oldQualifier.substring( 0, oldQualifier.length() - SNAPSHOT_SUFFIX.length() );
                oldQualifier = removeLastDelimiters( oldQualifier );
            }
            if ( !versionStringDelimiters.contains( partialSuffix.charAt( 0 ) ) && !isEmpty( oldQualifier ) )
            {
                partialSuffix = "-" + partialSuffix;
            }
            qualifierBase = oldQualifier + partialSuffix;
            buildNumber = null;
        }

        if ( newBuildNumber.length() > 0 )
        {
            buildNumber = newBuildNumber.toString();
            logger.debug( "Updated build number to: " + buildNumber );
        }

        updateQualifier();
    }

    /**
     * Appends a build number to the qualifier. The build number should be a string of digits, or the string "SNAPSHOT".
     * 
     * @param buildNumber
     */
    public void setBuildNumber( String buildNumber )
    {
        if ( buildNumber == null || isNumeric( buildNumber ) )
        {
            logger.debug( "replace build number " + this.buildNumber + " with " + buildNumber );
            this.buildNumber = buildNumber;
            updateQualifier();
        }
    }

    /**
     * Sets the snapshot to "SNAPSHOT" if true, otherwise sets to null
     * 
     * @param snapshot
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
        updateQualifier();
    }

    /**
     * Matches a version object to versions in a set by comparing the non build number portion of the string. Then find
     * which of the matching versions has the highest build number and is therefore the latest version.
     * 
     * @param version
     * @param versionSet
     * @return The highest build number, or 0 if no matching build numbers are found.
     */
    public int findHighestMatchingBuildNumber( Version version, Set<String> versionSet )
    {
        int highestBuildNum = 0;

        // Build version pattern regex
        StringBuffer versionPatternBuf = new StringBuffer();
        versionPatternBuf.append( "(" );
        versionPatternBuf.append( Pattern.quote( getOriginalMMM() ) );
        versionPatternBuf.append( ")?" );
        versionPatternBuf.append( DELIMITER_REGEX );
        if ( version.getQualifierBase() != null )
        {
            versionPatternBuf.append( Pattern.quote( version.getQualifierBase() ) );
            versionPatternBuf.append( DELIMITER_REGEX );
        }
        versionPatternBuf.append( "(" );
        versionPatternBuf.append( "\\d+" );
        versionPatternBuf.append( ")" );
        String candidatePatternStr = versionPatternBuf.toString();

        logger.debug( "Using pattern: '{}' to find compatible versions from metadata.", candidatePatternStr );
        final Pattern candidateSuffixPattern = Pattern.compile( candidatePatternStr );

        for ( final String compareVersion : versionSet )
        {
            final Matcher candidateSuffixMatcher = candidateSuffixPattern.matcher( compareVersion );
            if ( candidateSuffixMatcher.matches() )
            {
                String buildNumberStr = candidateSuffixMatcher.group( 2 );
                int compareBuildNum = Integer.parseInt( buildNumberStr );
                if ( compareBuildNum > highestBuildNum )
                {
                    highestBuildNum = compareBuildNum;
                }
            }
        }
        return highestBuildNum;
    }
}
