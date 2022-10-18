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

import lombok.Getter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.annotation.ConfigValue;
import org.commonjava.maven.ext.core.util.IdUtils;

import java.util.List;
import java.util.Properties;

/**
 * Captures configuration relating to dependency injection from the POMs.
 */
public class DependencyInjectionState
    implements State
{
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
    private List<ProjectVersionRef> dependencyInjection;

    @Getter
    private boolean addIgnoreUnusedAnalzyePlugin;

    public DependencyInjectionState( final Properties userProps)
    {
        initialise( userProps );
    }

    public void initialise( Properties userProps )
    {
        dependencyInjection = IdUtils.parseGAVs( userProps.getProperty( DEPENDENCY_INJECTION_PROPERTY ) );
        addIgnoreUnusedAnalzyePlugin =
                        Boolean.parseBoolean( userProps.getProperty( DEPENDENCY_INJECTION_ANALYZE_PLUGIN_PROPERTY,
                                                                     "false" ) );
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
