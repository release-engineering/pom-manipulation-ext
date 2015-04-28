package org.commonjava.maven.ext.manip.fixture;

import java.util.Arrays;
import java.util.Collections;

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
