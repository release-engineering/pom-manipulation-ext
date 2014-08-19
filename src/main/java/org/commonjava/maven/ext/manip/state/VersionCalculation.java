package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.ext.manip.impl.VersionCalculator;

public class VersionCalculation
{

    private final String originalVersion;

    private String baseVersion;

    private String suffix;

    private boolean snapshot;

    private int incrementalQualifier = -1;

    private String suffixSeparator = "-";

    private String baseVersionSeparator = "-";

    public VersionCalculation( final String originalVersion, final String baseVersion )
    {
        this.originalVersion = originalVersion;
        this.baseVersion = trimBaseVersion( baseVersion );
    }

    public void setIncrementalQualifier( final int qualifier )
    {
        this.incrementalQualifier = qualifier;
    }

    public void setSuffixSeparator( final String suffixSeparator )
    {
        this.suffixSeparator = suffixSeparator;
    }

    public void setVersionSuffix( final String suffix )
    {
        this.suffix = suffix;
    }

    private String trimBaseVersion( final String baseVersion )
    {
        // Now, pare back the trimmed version base to remove non-alphanums
        // like '.' and '-' so we have more control over them...
        int trim = 0;

        // calculate the trim size
        for ( int i = baseVersion.length() - 1; i > 0 && !Character.isLetterOrDigit( baseVersion.charAt( i ) ); i-- )
        {
            trim++;
        }

        // perform the actual trim to get back to an alphanumeric ending.
        if ( trim > 0 )
        {
            return baseVersion.substring( 0, baseVersion.length() - trim );
        }

        return baseVersion;
    }

    public void clear()
    {
        this.baseVersion = null;
    }

    public void setBaseVersionSeparator( final String baseVersionSeparator )
    {
        this.baseVersionSeparator = baseVersionSeparator;
    }

    public void setSnapshot( final boolean snapshot )
    {
        this.snapshot = snapshot;
    }

    public String renderVersion()
    {
        if ( baseVersion == null )
        {
            return originalVersion;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append( baseVersion );
        sb.append( baseVersionSeparator );
        sb.append( suffix );
        if ( incrementalQualifier > 0 )
        {
            sb.append( suffixSeparator )
              .append( incrementalQualifier );
        }

        if ( snapshot )
        {
            sb.append( VersionCalculator.SNAPSHOT_SUFFIX );
        }

        return sb.toString();
    }

    public int getIncrementalQualifier()
    {
        return incrementalQualifier;
    }

    public boolean hasCalculation()
    {
        return baseVersion != null;
    }

    public boolean isIncremental()
    {
        return incrementalQualifier > 0;
    }

    public void setBaseVersion( final String baseVersion )
    {
        this.baseVersion = trimBaseVersion( baseVersion );
    }

}
