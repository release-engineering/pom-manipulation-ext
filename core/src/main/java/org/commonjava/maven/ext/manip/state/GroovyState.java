/**
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
import org.commonjava.maven.ext.manip.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * Captures configuration relating to groovy script execution.
 */
public class GroovyState
    implements State
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * The name of the property which contains a comma separated list of remote groovy scripts to load.
     * <pre>
     * <code>-DpluginRemoval=org.foo:bar-plugin,....</code>
     * </pre>
     */
    private static final String GROOVY_SCRIPT = "groovyScripts";

    private final List<ArtifactRef> groovyScripts;

    public GroovyState( final Properties userProps )
    {
        groovyScripts = IdUtils.parseGAVTCs( userProps.getProperty( GROOVY_SCRIPT ) );
    }

    /**
     * Enabled ONLY if groovyScripts is provided in the user properties / CLI -D options.
     *
     * @see State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return groovyScripts != null && !groovyScripts.isEmpty();
    }

    public List<ArtifactRef> getGroovyScripts()
    {
        return groovyScripts;
    }
}
