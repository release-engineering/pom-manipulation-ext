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
package org.commonjava.maven.ext.manip.util;

import junit.framework.TestCase;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PropertiesUtilsTest extends TestCase
{

    @Test
    public void testCheckStrictValue() throws Exception
    {
        ManipulationSession session = new ManipulationSession();
        assertFalse(PropertiesUtils.checkStrictValue(session, null, "1.0"));
        assertFalse(PropertiesUtils.checkStrictValue(session, "1.0", null));
    }

    @Test
    public void testCacheProperty() throws Exception
    {
        Map propertyMap = new HashMap();
        
        assertFalse(PropertiesUtils.cacheProperty(propertyMap, null, "2.0", null));
        assertFalse(PropertiesUtils.cacheProperty(propertyMap, "1.0", "2.0", null));
        assertTrue(PropertiesUtils.cacheProperty(propertyMap, "${version.org.jboss}", "2.0", null));
    }

}