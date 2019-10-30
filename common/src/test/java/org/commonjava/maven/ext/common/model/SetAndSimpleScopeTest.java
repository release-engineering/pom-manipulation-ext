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
package org.commonjava.maven.ext.common.model;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.SimpleArtifactRef;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SetAndSimpleScopeTest
{
    @Test
    public void testVerifyArtifactRef()
    {
        SimpleArtifactRef commonslang = SimpleArtifactRef.parse( "commons-lang:commons-lang:jar:1.0" );
        SimpleArtifactRef commonslangsources = SimpleArtifactRef.parse( "commons-lang:commons-lang:jar:1.0:sources" );

        Set<ArtifactRef> s = new HashSet<>(  );
        s.add( commonslang );
        s.add( commonslangsources );

        assertEquals( 2, s.size() );
    }
}
