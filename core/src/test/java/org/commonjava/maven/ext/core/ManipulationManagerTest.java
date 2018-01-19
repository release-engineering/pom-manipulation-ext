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
package org.commonjava.maven.ext.core;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.core.fixture.PlexusTestRunner;
import org.commonjava.maven.ext.core.impl.Manipulator;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith( PlexusTestRunner.class )
@Component( role = ManipulationManagerTest.class )
public class ManipulationManagerTest
{
    @Requirement
    private Map<String, Manipulator> manipulators;

    @Test
    public void testListManipulators()
    {
        assertNotNull( manipulators );

        for ( final Map.Entry<String, Manipulator> entry : manipulators.entrySet() )
        {
            assertTrue (entry.getValue().getExecutionIndex() > 0 && entry.getValue().getExecutionIndex() < 100);
        }
    }
}
