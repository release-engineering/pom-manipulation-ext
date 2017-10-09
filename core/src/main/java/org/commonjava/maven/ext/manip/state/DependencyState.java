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
package org.commonjava.maven.ext.manip.state;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.impl.DependencyManipulator;
import org.commonjava.maven.ext.manip.util.IdUtils;
import org.commonjava.maven.ext.manip.util.PropertiesUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.commonjava.maven.ext.manip.util.PropertiesUtils.getPropertiesByPrefix;

/**
 * Captures configuration relating to dependency alignment from the POMs. Used by {@link DependencyManipulator}.
 */
public class DependencyState
                implements State
{
    /**
     * The String that needs to be prepended a system property to make it a dependencyExclusion.
     * For example to exclude junit alignment for the GAV (org.groupId:artifactId)
     * <pre>
     * <code>-DdependencyExclusion.junit:junit@org.groupId:artifactId</code>
     * </pre>
     */
    private static final String DEPENDENCY_EXCLUSION_PREFIX = "dependencyExclusion.";

    /**
     * Defines how dependencies are located.
     */
    private static final String DEPENDENCY_SOURCE = "dependencySource";

    /**
     * Merging precedence for dependency sources:
     * <pre>
     * <code>BOM</code> Solely Remote POM i.e. BOM.
     * <code>REST</code> Solely restURL.
     * <code>RESTBOM</code> Merges the information but takes the rest as precedence.
     * <code>BOMREST</code> Merges the information but takes the bom as precedence.
     * </pre>
     * Configured by the property <code>-DdependencySource=[REST|BOM|RESTBOM|BOMREST]</code>
     */
    public enum DependencyPrecedence
    {
        REST,
        BOM,
        RESTBOM,
        BOMREST
    }
    /**
     * Merely an alias for {@link DependencyState#DEPENDENCY_EXCLUSION_PREFIX}
     */
    private static final String DEPENDENCY_OVERRIDE_PREFIX = "dependencyOverride.";

    // TODO: ### the next 3 options probably need to be in common state to be shared by PluginState
    /**
     * Enables strict checking of non-exclusion dependency versions before aligning to the given BOM dependencies.
     * For example, <code>1.1</code> will match <code>1.1-rebuild-1</code> in strict mode, but <code>1.2</code> will not.
     */
    private static final String STRICT_DEPENDENCIES = "strictAlignment";

    /**
     * When false, strict version-alignment violations will be reported in the warning log-level, but WILL NOT FAIL THE BUILD. When true, the build
     * will fail if such a violation is detected. Default value is false.
     */
    private static final String STRICT_VIOLATION_FAILS = "strictViolationFails";

    /**
     * When true, it will ignore any suffix ( e.g. rebuild-2 ) on the source version during comparisons. Further, it will
     * only allow alignment to a higher incrementing suffix (e.g. rebuild-3 ).
     */
    public static final String STRICT_ALIGNMENT_IGNORE_SUFFIX = "strictAlignmentIgnoreSuffix";

    /**
     * The name of the property which contains the GAV of the remote pom from which to retrieve dependency management
     * information.
     * <pre>
     * <code>-DdependencyManagement:org.foo:bar-dep-mgmt:1.0</code>
     * </pre>
     */
    private static final String DEPENDENCY_MANAGEMENT_POM_PROPERTY = "dependencyManagement";

    private final boolean overrideDependencies;

    private final boolean strict;

    private final boolean failOnStrictViolation;

    private final boolean ignoreSuffix;

    private final List<ProjectVersionRef> remoteBOMdepMgmt;

    private Map<String, String> dependencyExclusions;

    private Map<ArtifactRef, String> remoteRESTdepMgmt;

    private DependencyPrecedence precedence;

    public DependencyState( final Properties userProps ) throws ManipulationException
    {
        overrideDependencies = Boolean.valueOf( userProps.getProperty( "overrideDependencies", "true" ) );
        strict = Boolean.valueOf( userProps.getProperty( STRICT_DEPENDENCIES, "false" ) );
        ignoreSuffix = Boolean.valueOf( userProps.getProperty( STRICT_ALIGNMENT_IGNORE_SUFFIX, "false" ) );
        failOnStrictViolation = Boolean.valueOf( userProps.getProperty( STRICT_VIOLATION_FAILS, "false" ) );
        remoteBOMdepMgmt = IdUtils.parseGAVs( userProps.getProperty( DEPENDENCY_MANAGEMENT_POM_PROPERTY ) );
        dependencyExclusions = getPropertiesByPrefix( userProps, DEPENDENCY_EXCLUSION_PREFIX );

        Map<String, String> oP = PropertiesUtils.getPropertiesByPrefix( userProps, DEPENDENCY_OVERRIDE_PREFIX );
        for ( String s : oP.keySet() )
        {
            if ( dependencyExclusions.put( s, oP.get( s ) ) != null )
            {
                throw new ManipulationException( "Property clash between dependencyOverride and dependencyExclusion for " + s );
            }
        }
        switch ( DependencyPrecedence.valueOf( userProps.getProperty( DEPENDENCY_SOURCE,
                                                            DependencyPrecedence.BOM.toString() ).toUpperCase() ) )
        {
            case REST:
            {
                precedence = DependencyPrecedence.REST;
                break;
            }
            case BOM:
            {
                precedence = DependencyPrecedence.BOM;
                break;
            }
            case RESTBOM:
            {
                precedence = DependencyPrecedence.RESTBOM;
                break;
            }
            case BOMREST:
            {
                precedence = DependencyPrecedence.BOMREST;
                break;
            }
            default:
            {
                throw new ManipulationException( "Unknown value {} for {}", userProps.getProperty( DEPENDENCY_SOURCE ), DEPENDENCY_SOURCE);
            }
        }
    }

    /**
     * Enabled ONLY if dependencyManagement is provided OR restURL has been provided.
     *
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return ( ( remoteBOMdepMgmt != null && !remoteBOMdepMgmt.isEmpty()   ) ||
                 ( remoteRESTdepMgmt != null && !remoteRESTdepMgmt.isEmpty() ) ||
                 (!dependencyExclusions.isEmpty()) );
    }

    public List<ProjectVersionRef> getRemoteBOMDepMgmt()
    {
        return remoteBOMdepMgmt;
    }

    /**
     * @return whether to override managed dependencies in the build. Defaults to true.
     * @deprecated
     */
    public boolean getOverrideDependencies()
    {
        return overrideDependencies;
    }

    public boolean getStrict()
    {
        return strict;
    }

    public boolean getStrictIgnoreSuffix()
    {
        return ignoreSuffix;
    }

    public boolean getFailOnStrictViolation()
    {
        return failOnStrictViolation;
    }

    public DependencyPrecedence getPrecedence()
    {
        return precedence;
    }

    public void setRemoteRESTOverrides( Map<ArtifactRef, String> overrides )
    {
        remoteRESTdepMgmt = overrides;
    }

    public Map<ArtifactRef, String> getRemoteRESTOverrides( )
    {
        if ( remoteRESTdepMgmt == null )
        {
            remoteRESTdepMgmt = new HashMap<>(  );
        }
        return remoteRESTdepMgmt;
    }

    public Map<String, String> getDependencyExclusions( )
    {
        return dependencyExclusions;
    }

    public void updateExclusions (String key, String value)
    {
        dependencyExclusions.put( key, value );
    }
}
