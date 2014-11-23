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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that reads a version string and makes various modifications to it such as converting to a valid OSGi
 * version string and/or incrementing a version suffix. See: http://www.aqute.biz/Bnd/Versioning for an explanation of
 * OSGi versioning. Parses versions into the following format:
 * &lt;major&gt;.&lt;minor&gt;.&lt;micro&gt;.&lt;qualifierBase&gt;-&lt;suffix&gt;-&lt;buildnumber&gt;
 */
public class Version
{

    private final Character[] DEFAULT_DELIMITERS = { '.', '-', '_' };

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

    /**
     * Has the qualifier suffix or build number been changed from the original version string
     */
    private boolean qualifierChanged;

    private String qualifierBase;

    private String qualifierSuffix;

    /**
     * Numeric string at the end of the qualifier
     */
    private String buildNumber;

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
            getMicroVersion() + ", Qualifier: " + getQualifier() + ", Suffix: " + getQualifierSuffix() );
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
        String qualifierBase = qualifier;
        List<String> qualifierParts = new ArrayList<String>();

        // Break the qualifier into sections
        while ( !isEmpty( remainingQualString ) )
        {
            remainingQualString = removeNextDelimiters( remainingQualString );
            String nextVersionPart = getNextVersionPart( remainingQualString );
            qualifierParts.add( nextVersionPart );
            remainingQualString = remainingQualString.substring( ( nextVersionPart.length() ) );
        }

        // Try to extract the build number
        String lastQualifierPart = qualifierParts.get( qualifierParts.size() - 1 );
        if ( isNumeric( lastQualifierPart ) || SNAPSHOT_SUFFIX.equalsIgnoreCase( lastQualifierPart ) )
        {
            buildNumber = lastQualifierPart;
            qualifierBase = qualifierBase.substring( 0, qualifierBase.length() - buildNumber.length() );
            qualifierBase = removeLastDelimiters( qualifierBase );
            qualifierParts.remove( qualifierParts.size() - 1 );
        }

        // Try to extract the qualifier suffix
        if ( qualifierParts.size() > 0 )
        {
            lastQualifierPart = qualifierParts.get( qualifierParts.size() - 1 );
            if ( !isNumeric( lastQualifierPart ) )
            {
                qualifierSuffix = lastQualifierPart;
                qualifierBase = qualifierBase.substring( 0, qualifierBase.length() - qualifierSuffix.length() );
                qualifierBase = removeLastDelimiters( qualifierBase );
            }
        }

        this.qualifierBase = qualifierBase;
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
        if ( !this.qualifierChanged )
        {
            qualifier = originalQualifier;
            return;
        }
        StringBuilder updatedQualifier = new StringBuilder();
        if ( !isEmpty( getQualifierBase() ) )
        {
            updatedQualifier.append( getQualifierBase() );
            if ( !isEmpty( getQualifierSuffix() ) || !isEmpty( getBuildNumber() ) )
            {
                updatedQualifier.append( '-' );
            }
        }

        if ( !isEmpty( getQualifierSuffix() ) )
        {
            updatedQualifier.append( getQualifierSuffix() );
            if ( !isEmpty( getBuildNumber() ) )
            {
                updatedQualifier.append( '-' );
            }
        }

        if ( !isEmpty( getBuildNumber() ) )
        {
            updatedQualifier.append( getBuildNumber() );
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
     * Generate the qualifier by combining the qualifierBase, qualifierSuffix, and build number
     * 
     * @return The qualifier part of the version string (an empty string if there is no qualifier)
     */
    public String getQualifier()
    {
        if ( qualifier != null )
        {
            return qualifier;
        }
        return originalQualifier;
    }

    public String getQualifierBase()
    {
        if ( qualifierBase == null )
        {
            qualifierBase = originalQualifier;
        }
        return qualifierBase;
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
     * Get a three part OSGi version with an optional qualifier. This method will force the version string to contain a
     * major, minor, and micro version. So "1.2" will become "1.2.0".
     * 
     * @return
     */
    public String getOSGiVersionStringMaximized()
    {
        StringBuilder osgiVerMaxed = new StringBuilder();

        if ( numericVersion )
        {
            osgiVerMaxed.append( getMajorVersion() );
            osgiVerMaxed.append( OSGI_VERSION_DELIMITER );
            osgiVerMaxed.append( getMinorVersion() );
            osgiVerMaxed.append( OSGI_VERSION_DELIMITER );
            osgiVerMaxed.append( getMicroVersion() );
        }
        if ( this.hasQualifier() )
        {
            if ( numericVersion )
            {
                osgiVerMaxed.append( OSGI_VERSION_DELIMITER );
            }
            osgiVerMaxed.append( getOSGiQualifier() );
        }

        return osgiVerMaxed.toString();
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
        return SNAPSHOT_SUFFIX.equalsIgnoreCase( getBuildNumber() );
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
     * Returns the last alpha part of the qualifier
     * 
     * @return
     */
    public String getQualifierSuffix()
    {
        return qualifierSuffix;
    }

    /**
     * Sets the qualifier suffix to the current version. If the suffix matches the existing one, does nothing
     * 
     * @param suffix The qualifier suffix to append. This can be a simple string like "foo", or it can optionally
     *            include a build number, for example "foo-1", which will automatically be set as the build number for
     *            this version.
     */
    public void setQualifierSuffix( String suffix )
    {
        if ( suffix == null )
        {
            return;
        }
        suffix = removeNextDelimiters( suffix );

        StringBuilder newBuildNumber = new StringBuilder();
        while ( isNumeric( suffix.substring( suffix.length() - 1 ) ) )
        {
            newBuildNumber.insert( 0, suffix.substring( suffix.length() - 1 ) );
            suffix = suffix.substring( 0, suffix.length() - 1 );
        }

        suffix = this.removeLastDelimiters( suffix );

        if ( !suffix.equals( qualifierSuffix ) )
        {
            qualifierSuffix = suffix;
            qualifierChanged = true;
            qualifierBase = originalQualifier;
            buildNumber = null;
        }
        if ( newBuildNumber.length() > 0 )
        {
            qualifierChanged = true;
            buildNumber = newBuildNumber.toString();
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
        if ( buildNumber == null )
        {
            if ( this.buildNumber != null )
            {
                qualifierChanged = true;
                this.buildNumber = null;
            }
            return;
        }

        if ( !isNumeric( buildNumber ) && !buildNumber.equals( SNAPSHOT_SUFFIX ) )
        {
            return;
        }

        if ( this.buildNumber == null || !this.buildNumber.equals( buildNumber ) )
        {
            this.buildNumber = buildNumber;
            qualifierChanged = true;
            updateQualifier();
        }
    }
}
