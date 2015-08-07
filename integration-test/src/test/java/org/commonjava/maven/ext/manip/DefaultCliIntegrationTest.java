/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.commonjava.maven.ext.manip;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.commonjava.maven.ext.manip.CliTestUtils.*;

@SuppressWarnings("ConstantConditions")
@RunWith(Parameterized.class)
public class DefaultCliIntegrationTest {
    private static final List<String> EXCLUDED_FILES = new ArrayList<String>() {{
        add("setup");
    }};

    @Parameters
    public static Collection<Object[]> getFiles() {
        Collection<Object[]> params = new ArrayList<Object[]>();
        for (File rl : new File(IT_LOCATION).listFiles()) {
            if (rl.isDirectory() && !EXCLUDED_FILES.contains(rl.getName())) {
                Object[] arr = new Object[] { rl.toString() };
                params.add(arr);
            }
        }
        return params;
    }

    private String testLocation;

    public DefaultCliIntegrationTest(String testLocation) {
        this.testLocation = testLocation;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        for (File setupTest : new File(getDefaultTestLocation("setup")).listFiles()) {
            runMaven("install", DEFAULT_MVN_PARAMS, setupTest.toString());
        }
    }

    @Test
    public void testIntegration() throws Exception {
        runCli(testLocation);
        runMaven("install", DEFAULT_MVN_PARAMS, testLocation);
        verify(testLocation);
    }
}