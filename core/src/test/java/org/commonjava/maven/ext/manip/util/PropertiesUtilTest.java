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

import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;

import junit.framework.TestCase;

import org.commonjava.maven.ext.manip.ManipulationSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class PropertiesUtilTest extends TestCase
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