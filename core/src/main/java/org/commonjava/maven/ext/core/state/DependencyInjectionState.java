/*
 * Copyright (C) 2012 Red Hat, Inc.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.annotation.ConfigValue;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

import lombok.Getter;

/**
 * Captures configuration relating to dependency injection from the POMs.
 */
public class DependencyInjectionState
    implements State
{
    private static final Logger logger = LoggerFactory.getLogger( DependencyInjectionState.class );

    /**
     * The name of the property which contains a comma separated list of dependencies (as GAV) to inject.
     * <pre>
     * <code>-DdependencyInjection=org.foo:bar:1.0,....</code>
     * </pre>
     */
    @ConfigValue( docIndex = "dep-manip.html#dependency-injection")
    private static final String DEPENDENCY_INJECTION_PROPERTY = "dependencyInjection";

    /**
     * This will update the
     * <a href="https://maven.apache.org/plugins/maven-dependency-plugin/analyze-mojo.html#dependency-analyze">maven-dependency-plugin</a>
     * plugin so that a section with ignoredUnusedDeclaredDependencies is added for each dependency if the
     * maven-dependency-plugin is declared in the root pom.
     */
    @ConfigValue( docIndex = "dep-manip.html#dependency-injection-assembly")
    private static final String DEPENDENCY_INJECTION_ANALYZE_PLUGIN_PROPERTY = "dependencyInjectionAnalyzeIgnoreUnused";

    /**
     * @return the dependencies we wish to remove.
     */
    @Getter
    private List<Dependency> dependencyInjection;

    @Getter
    private boolean addIgnoreUnusedAnalzyePlugin;

    public DependencyInjectionState( final Properties userProps)
    {
        initialise( userProps );
    }

    public void initialise( Properties userProps )
    {
        dependencyInjection = parseDependencies( userProps.getProperty( DEPENDENCY_INJECTION_PROPERTY ) );
        addIgnoreUnusedAnalzyePlugin =
                        Boolean.parseBoolean( userProps.getProperty( DEPENDENCY_INJECTION_ANALYZE_PLUGIN_PROPERTY,
                                                                     "false" ) );
    }

    /**
     * Splits the value on ',', then wraps each value in {@link SimpleProjectVersionRef#parse(String)}. Returns null
     * if the input value is null.
     * @param value a comma separated list of GAV to parse
     * @return a collection of parsed ProjectVersionRef.
     */
    private static List<Dependency> parseDependencies( final String value )
    {
        if ( isEmpty( value ) )
        {
            return null;
        }
        else
        {
            final String[] depCoords = value.split( "," );
            final List<Dependency> deps = new ArrayList<>();
            for ( final String dep : depCoords )
            {
                if (isNotEmpty( dep ))
                {
                    if ( dep.startsWith( "http://" ) || dep.startsWith( "https://") )
                    {
                        logger.debug( "Found remote file in {}", dep );
                        try
                        {
                            File found = File.createTempFile( UUID.randomUUID().toString(), null );
                            FileUtils.copyURLToFile( new URL( dep ), found );
                            String potentialRefs =
                                            FileUtils.readFileToString( found, Charset.defaultCharset() ).trim().replace( "\n", "," );
                            List<Dependency> readRefs = parseDependencies( potentialRefs );
                            if ( readRefs != null )
                            {
                                deps.addAll( readRefs );
                            }
                        }
                        catch ( InvalidRefException | IOException e )
                        {
                            throw new ManipulationUncheckedException( e );
                        }
                    }
                    else
                    {
                        final String[] parts = dep.split( ":" );
                        final Dependency d = new Dependency();

                        if ( parts.length < 3 || isEmpty( parts[0] ) || isEmpty( parts[1] ) || isEmpty( parts[2] ) )
                        {
                            throw invalidRefException();
                        }

                        d.setGroupId(parts[0]);
                        d.setArtifactId(parts[1]);

                        switch ( parts.length ) {
                            case 3:
                                d.setVersion(parts[2]);
                                break;
                            case 4:
                                d.setType(parts[2]);
                                d.setVersion(parts[3]);
                                break;
                            case 5:
                                d.setType(parts[2]);
                                d.setClassifier(parts[3]);
                                d.setVersion(parts[4]);
                                break;
                            case 6:
                                d.setType(parts[2]);
                                d.setClassifier(parts[3]);
                                d.setVersion(parts[4]);
                                d.setScope(parts[5]);
                                break;
                            default:
                                throw invalidRefException();
                        }

                        deps.add( d );
                    }
                }
            }
            return deps;
        }
    }

    static InvalidRefException invalidRefException() {
        return new InvalidRefException( "dependency must contain be formatted as: "
                + "groupId:artifactId:version, "
                + "groupId:artifactId:type:version, "
                + "groupId:artifactId:type:classifier:version, or"
                + "groupId:artifactId:type:classifier:version:scope" );
    }

    /**
     * Enabled ONLY if dependency-removal is provided in the user properties / CLI -D options.
     *
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return dependencyInjection != null && !dependencyInjection.isEmpty();
    }
}
