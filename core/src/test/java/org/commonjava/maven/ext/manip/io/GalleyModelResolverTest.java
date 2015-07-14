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
package org.commonjava.maven.ext.manip.io;

import org.apache.maven.model.resolution.UnresolvableModelException;
import org.commonjava.maven.ext.manip.resolver.GalleyAPIWrapper;
import org.commonjava.maven.ext.manip.resolver.GalleyInfrastructure;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

@RunWith(BMUnitRunner.class)
public class GalleyModelResolverTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test(expected = UnresolvableModelException.class)
    @BMRule(name = "retrieve-first-null",
                    targetClass = "ArtifactManagerImpl",
                    targetMethod = "retrieveFirst(List<? extends Location> locations, ArtifactRef ref)",
                    targetLocation = "AT ENTRY",
                    action = "RETURN null"
    )
    public void resolveArtifactTest()
        throws Exception
    {
        final ManipulationSession session = new ManipulationSession();
        final GalleyInfrastructure galleyInfra =
            new GalleyInfrastructure( session, null, null, null, temp.newFolder( "cache-dir" ) );
        final GalleyAPIWrapper wrapper = new GalleyAPIWrapper( galleyInfra );
        final GalleyModelResolver gm = new GalleyModelResolver(wrapper);

        gm.resolveModel("org.commonjava", "commonjava", "5");
    }
}
