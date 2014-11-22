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
 * &lt;major&gt;.&lt;minor&gt;.&lt;micro&gt;.&lt;qualifier&gt;-&lt;suffix&gt;-&lt;buildnumber&gt;
 */
public class Version
{

    private final Character[] DEFAULT_DELIMITERS = { '.', '-', '_' };

    private final char OSGI_VERSION_DELIMITER = '.';

    // Used to match valid osgi version
    private static final String OSGI_VERSION_REGEX = "(\\d+)(\\.\\d+(\\.\\d+([\\.][\\p{Alnum}|\\-|_]+)?)?)?";

    private final String SNAPSHOT_SUFFIX = "SNAPSHOT";

    private List<Character> versionStringDelimiters = Arrays.asList( DEFAULT_DELIMITERS );

    private List<String> versionParts;

    /**
     * The original version string before any modifications
     */
    private final String originalVersion;

    /**
     * The original unmodified version qualifier.  Will be null if no qualifier is included.
     */
    private String originalQualifier;

    /**
     * The current qualifier, after any modifications such as suffix or build number changes
     * have been made.
     */
    private String qualifier;
    /**
     * Has the qualifier suffix or build number been changed from the original version string
     */
    private boolean qualifierChanged;

    private String qualifierBase;

    private String qualifierSuffix;

    private String buildNumber;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

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
        List<String> versionParts = new ArrayList<String>();

        String remainingVersionString = version;

        boolean foundQualifier = false;

        // If the first character is a delimiter, then we put a zero as the major version
        if ( versionStringDelimiters.contains( version.charAt( 0 ) ) )
        {
            versionParts.add( "0" );
        }

        while ( !isEmpty( remainingVersionString ) )
        {
            // Remove the next delimiter(s) from the version
            remainingVersionString = removeNextDelimiters( remainingVersionString );
            String nextVersionPart = getNextVersionPart( remainingVersionString );

            // logger.debug( "version parts: " + versionParts );
            if ( ( !foundQualifier ) && ( !isNumeric( nextVersionPart ) || versionParts.size() == 3 ) )
            {
                foundQualifier = true;
                originalQualifier = removeNextDelimiters( remainingVersionString );

                // Add zeros for the minor and micro versions if necessary
                while ( versionParts.size() < 3 )
                {
                    versionParts.add( "0" );
                }
            }

            remainingVersionString = remainingVersionString.substring( ( nextVersionPart.length() ) );
            versionParts.add( nextVersionPart );
        }

        if ( !foundQualifier )
        {
            // If there is no qualifier, we might have a version like "1.2" and
            // we need to add a micro version of "0".
            while ( versionParts.size() < 3 )
            {
                versionParts.add( "0" );
            }
        }

        this.versionParts = versionParts;
        parseQualifier();
    }

    private void parseQualifier()
    {
        if ( hasQualifier() )
        {
            List<String> qualifierParts = new ArrayList<String>();
            for ( int i = 3; i < versionParts.size(); ++i )
            {
                qualifierParts.add( versionParts.get( i ) );
            }
            qualifierBase = originalQualifier;

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
        }
        updateQualifier();
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
            // logger.debug( "remaining: " + remainingVersionString + "  versionPart: " + versionPart + "  nextChar: " +
            // nextChar );
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
        return versionParts.get( 0 );
    }

    public String getMinorVersion()
    {
        return versionParts.get( 1 );
    }

    public String getMicroVersion()
    {
        return versionParts.get( 2 );
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
     * Generate the qualifier by combining the qualifierBase, qualifierSuffix, and build number
     * 
     * @return
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
        StringBuilder osgiVersion = new StringBuilder( getMajorVersion() );

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
        if ( !isEmpty( getQualifier() ) )
        {
            osgiVersion.append( OSGI_VERSION_DELIMITER );
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
     * Appends a qualifier suffix to the current version If the suffix matches the existing one, does nothing
     * 
     * @param suffix The qualifier suffix to append. This can be a simple string like "foo", or it can optionally
     *            include a build number, for example "foo-1", which will automatically be set as the build number for
     *            this version.
     */
    public void appendQualifierSuffix( String suffix )
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
