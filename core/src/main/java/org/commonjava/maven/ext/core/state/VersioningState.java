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
package org.commonjava.maven.ext.core.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.model.GAV;
import org.commonjava.maven.ext.core.impl.ProjectVersioningManipulator;
import org.commonjava.maven.ext.core.util.PropertiesUtils;
import org.commonjava.maven.ext.core.util.PropertyFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Captures configuration and changes relating to the projects' versions. Used by {@link ProjectVersioningManipulator}.
 *
 * @author jdcasey
 */
@Getter
public class VersioningState
    implements State
{
    public static final PropertyFlag VERSION_SUFFIX_SYSPROP = new PropertyFlag( "version.suffix", "versionSuffix");

    public static final PropertyFlag INCREMENT_SERIAL_SUFFIX_SYSPROP = new PropertyFlag( "version.incremental.suffix", "versionIncrementalSuffix");

    public static final PropertyFlag INCREMENT_SERIAL_SUFFIX_PADDING_SYSPROP = new PropertyFlag( "version.incremental.suffix.padding", "versionIncrementalSuffixPadding");

    public static final PropertyFlag VERSION_SUFFIX_SNAPSHOT_SYSPROP = new PropertyFlag( "version.suffix.snapshot", "versionSuffixSnapshot");

    public static final PropertyFlag VERSION_OSGI_SYSPROP = new PropertyFlag( "version.osgi", "versionOsgi");

    public static final PropertyFlag VERSION_OVERRIDE_SYSPROP = new PropertyFlag( "version.override", "versionOverride");

    public static final String VERSION_SUFFIX_ALT = "versionSuffixAlternatives";

    /**
     * @return the version suffix to be appended to the project version.
     */
    private final String suffix;

    /**
     * @return the incremental suffix that will be appended to the project version.
     */
    private final String incrementalSerialSuffix;

    /**
     * @return true if we should preserve the snapshot
     */
    private final boolean preserveSnapshot;

    /**
     * @return true if we should make the versions OSGi compliant
     */
    private final boolean osgi;

    /**
     * Forcibly override the version to a new one.
     * @return the new version
     */
    private final String override;

    /**
     * @return the incremental suffix padding that will be appended to the project version i.e. whether to append 001 or 1.
     */
    private final int incrementalSerialSuffixPadding;

    private final List<String> suffixAlternatives;

    private final List<String> allSuffixes;

    @JsonProperty
    private GAV executionRootModified;

    /**
     * Record the versions to change. Essentially this contains a mapping of original
     * project GAV to new version to change.
     */
    private final Map<ProjectVersionRef, String> versionsByGAV = new HashMap<>();

    /**
     * Store preprocessed metadata from the REST call in order to use for incremental lookup.
     */
    private Map<ProjectRef, Set<String>> restMetaData;

    public VersioningState( final Properties userProps )
    {
        suffix = PropertiesUtils.handleDeprecatedProperty( userProps, VERSION_SUFFIX_SYSPROP );
        incrementalSerialSuffix = PropertiesUtils.handleDeprecatedProperty( userProps, INCREMENT_SERIAL_SUFFIX_SYSPROP );
        incrementalSerialSuffixPadding = Integer.parseInt( PropertiesUtils.handleDeprecatedProperty( userProps, INCREMENT_SERIAL_SUFFIX_PADDING_SYSPROP, "0" ) );
        preserveSnapshot = Boolean.parseBoolean( PropertiesUtils.handleDeprecatedProperty( userProps, VERSION_SUFFIX_SNAPSHOT_SYSPROP ) );
        osgi = Boolean.parseBoolean( PropertiesUtils.handleDeprecatedProperty( userProps, VERSION_OSGI_SYSPROP, "true" ) );
        override = PropertiesUtils.handleDeprecatedProperty( userProps, VERSION_OVERRIDE_SYSPROP );

        // Provide an alternative list of versionSuffixes split via a comma separator. Defaults to 'redhat' IF the current rebuild suffix is not that.
        suffixAlternatives = Arrays.asList(
                        StringUtils.split( userProps.getProperty(
                                        VERSION_SUFFIX_ALT, "redhat".equals( getRebuildSuffix() ) ? "" : "redhat" ), "," ) );

        allSuffixes = new ArrayList<>( );

        // If no suffix is configured then don't fill in the all suffixes array.
        if ( isNotEmpty( getRebuildSuffix() ) )
        {
            allSuffixes.add( getRebuildSuffix() );
            allSuffixes.addAll( getSuffixAlternatives() );
        }
    }


    /**
     * Enabled ONLY if either version.incremental.suffix or version.suffix is provided in the user properties / CLI -D options.
     *
     * @see #VERSION_SUFFIX_SYSPROP
     * @see #INCREMENT_SERIAL_SUFFIX_SYSPROP
     * @see org.commonjava.maven.ext.core.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return incrementalSerialSuffix != null || suffix != null || override != null;
    }

    public void setRESTMetadata( Map<ProjectRef, Set<String>> versionStates )
    {
        restMetaData = versionStates;
    }

    public Map<ProjectRef, Set<String>> getRESTMetadata( )
    {
        return restMetaData;
    }

    public void setVersionsByGAVMap( Map<ProjectVersionRef, String> versionsByGAV )
    {
        this.versionsByGAV.putAll( versionsByGAV );
    }

    public void setExecutionRootModified( GAV executionRootModified )
    {
        this.executionRootModified = executionRootModified;
    }

    public boolean hasVersionsByGAV()
    {
        return versionsByGAV != null && !versionsByGAV.isEmpty();
    }

    public String getRebuildSuffix()
    {
        String suffix = "";

        // Same precedence as VersionCalculator.
        if ( ! isEmpty ( getSuffix() ) )
        {
            int dashIndex = getSuffix().lastIndexOf( '-' );
            suffix = getSuffix().substring( 0, dashIndex > 0 ? dashIndex : getSuffix().length() );
        }
        else if ( ! isEmpty ( getIncrementalSerialSuffix() ) )
        {
            suffix = getIncrementalSerialSuffix();
        }
        return suffix;
    }
}
