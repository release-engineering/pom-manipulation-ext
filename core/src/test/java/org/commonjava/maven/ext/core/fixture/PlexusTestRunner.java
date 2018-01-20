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
package org.commonjava.maven.ext.core.fixture;

import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;
import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.URLClassSpace;
import org.sonatype.guice.plexus.binders.PlexusAnnotatedBeanModule;
import org.sonatype.guice.plexus.config.PlexusBeanModule;

import java.util.Arrays;
import java.util.Collections;

public class PlexusTestRunner
    extends BlockJUnit4ClassRunner
{

    public PlexusTestRunner( final Class<?> klass )
        throws InitializationError
    {
        super( klass );
    }

    @Override
    protected Object createTest()
        throws Exception
    {
        final TestClass testClass = getTestClass();

        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();

        config.setAutoWiring( true );
        config.setClassPathScanning( PlexusConstants.SCANNING_ON );
        config.setComponentVisibility( PlexusConstants.GLOBAL_VISIBILITY );
        config.setName( testClass.getName() );

        final DefaultPlexusContainer container = new DefaultPlexusContainer( config );
        final ClassSpace cs = new URLClassSpace( Thread.currentThread()
                                                       .getContextClassLoader() );

        container.addPlexusInjector( Arrays.<PlexusBeanModule> asList( new PlexusAnnotatedBeanModule(
                                                                                                      cs,
                                                                                                      Collections.emptyMap() ) ) );

        return container.lookup( testClass.getJavaClass() );
    }

}
