/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

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
