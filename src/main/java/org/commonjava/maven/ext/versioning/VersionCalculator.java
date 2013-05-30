package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.ga;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.project.MavenProject;

public class VersionCalculator
{

    private static final String SERIAL_SUFFIX_PATTERN = "(.+)([-.])(\\d+)";

    public static final String VERSION_SUFFIX_SYSPROP = "version.suffix";

    public static final String INCREMENT_SERIAL_SYSPROP = "version.serial.increment";

    private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private final String suffix;

    private boolean incrementSerial = false;

    public VersionCalculator( final Properties properties )
    {
        suffix = properties.getProperty( VERSION_SUFFIX_SYSPROP );
        incrementSerial = Boolean.parseBoolean( properties.getProperty( INCREMENT_SERIAL_SYSPROP, "false" ) );
    }

    public boolean isEnabled()
    {
        return suffix != null;
    }

    public Map<String, String> calculateVersioningChanges( final Collection<MavenProject> projects )
    {
        final Map<String, String> versionsByGA = new HashMap<String, String>();

        for ( final MavenProject project : projects )
        {
            final String originalVersion = project.getVersion();
            final String modifiedVersion = calculate( originalVersion );

            if ( !modifiedVersion.equals( originalVersion ) )
            {
                versionsByGA.put( ga( project.getGroupId(), project.getArtifactId() ), modifiedVersion );
            }
        }

        return versionsByGA;
    }

    public String calculate( final String originalVersion )
    {
        String result = originalVersion;

        boolean snapshot = false;
        // If we're building a snapshot, make sure the resulting version ends 
        // in "-SNAPSHOT"
        if ( result.endsWith( SNAPSHOT_SUFFIX ) )
        {
            snapshot = true;
            result = result.substring( 0, result.length() - SNAPSHOT_SUFFIX.length() );
        }

        final Matcher suffixMatcher = Pattern.compile( SERIAL_SUFFIX_PATTERN )
                                             .matcher( suffix );

        String useSuffix = suffix;
        if ( suffixMatcher.matches() )
        {
            // the "redhat" in "redhat-1"
            final String suffixBase = suffixMatcher.group( 1 );
            final int idx = result.indexOf( suffixBase );

            String oldSuffix = null;
            if ( idx > -1 )
            {
                // grab the old suffix for serial increment, if necessary
                oldSuffix = result.substring( idx );

                // trim the old suffix off.
                result = result.substring( 0, idx );
            }

            // If we're using serial suffixes (-redhat-N) and the flag is set 
            // to increment the existing suffix, read the old one, increment it, 
            // and set the suffix to be used based on that.
            if ( incrementSerial && oldSuffix != null )
            {
                final Matcher oldSuffixMatcher = Pattern.compile( SERIAL_SUFFIX_PATTERN )
                                                        .matcher( oldSuffix );

                if ( oldSuffixMatcher.matches() )
                {
                    // don't assume we're using '-' as suffix-base-to-serial-number separator...
                    final String oldSep = oldSuffixMatcher.group( 2 );

                    // grab the old serial number.
                    final int oldSerial = Integer.parseInt( oldSuffixMatcher.group( 3 ) );
                    useSuffix = suffixBase + oldSep + ( oldSerial + 1 );
                }
            }

            // Now, pare back the trimmed version base to remove non-alphanums 
            // like '.' and '-' so we have more control over them...
            int trim = 0;

            // calculate the trim size
            for ( int i = result.length() - 1; i > 0 && !Character.isLetterOrDigit( result.charAt( i ) ); i-- )
            {
                trim++;
            }

            // perform the actual trim to get back to an alphanumeric ending.
            if ( trim > 0 )
            {
                result = result.substring( 0, result.length() - trim );
            }
        }
        // If we're not using a serial suffix, and the version already ends 
        // with the chosen suffix, there's nothing to do! 
        else if ( originalVersion.endsWith( suffix ) )
        {
            return originalVersion;
        }

        // assume the version is of the form 1.2.3.GA, where appending the 
        // suffix requires a '-' to concatenate the string of the final version 
        // part in OSGi.
        String sep = "-";

        // now, check the above assumption...
        // if the version is of the form: 1.2.3, then we need to append the 
        // suffix as a final version part using '.'
        if ( Character.isDigit( result.charAt( result.length() - 1 ) ) )
        {
            sep = ".";
        }

        // TODO OSGi fixup for versions like 1.2.GA or 1.2 (too few parts)

        result += sep + useSuffix;

        // tack -SNAPSHOT back on if necessary...
        if ( snapshot )
        {
            result += SNAPSHOT_SUFFIX;
        }

        return result;
    }

    public String getSuffix()
    {
        return suffix;
    }

}
