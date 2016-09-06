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
package org.commonjava.maven.ext.manip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.maven.ext.manip.rest.handler.AddSuffixJettyHandler;
import org.commonjava.maven.ext.manip.rest.rule.MockServer;
import org.commonjava.maven.ext.manip.state.VersioningState;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.codehaus.plexus.util.FileUtils.copyFile;
import static org.commonjava.maven.ext.manip.TestUtils.runCli;
import static org.junit.Assert.*;

/**
 * Manipulators may output data in a structured form in a JSON file
 * for further processing, by allowing some of the state fields to be serialized.
 * This test verifies that this works in case of {@link VersioningState#executionRootModified}.
 *
 * @author Jakub Senko
 */
public class ResultJsonFileTest
{
    @ClassRule public static MockServer mockServer = new MockServer( new AddSuffixJettyHandler() );

    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Rule public TemporaryFolder tmpFolderRule = new TemporaryFolder();

    @Test public void testVersioningStateOutputJsonFile()
                    throws Exception
    {
        // given

        File baseDir = tmpFolderRule.newFolder();
        File pomFile = getResourceFile( "result-json-test/pom.xml" );
        copyFile( pomFile, new File( baseDir, "pom.xml" ) );

        // when

        Map<String, String> params = new HashMap<>();
        params.put( "restURL", mockServer.getUrl() );
        params.put( "version.incremental.suffix", "redhat" );

        Integer exitValue = runCli( (List<String>) (List<?>) emptyList(), params, baseDir.getCanonicalPath() );

        // then

        assertEquals( (Integer) 0, exitValue );

        File outputJsonFile = new File( baseDir, "/target/pom-manip-ext-result.json" );
        assertTrue( outputJsonFile.exists() );

        JsonNode rootNode = MAPPER.readTree( outputJsonFile );

        JsonNode versioningState = rootNode.get( "VersioningState" );
        assertNotNull( versioningState );

        JsonNode executionRootModified = versioningState.get( "executionRootModified" );
        assertNotNull( executionRootModified );

        JsonNode groupId = executionRootModified.get( "groupId" );
        JsonNode artifactId = executionRootModified.get( "artifactId" );
        JsonNode version = executionRootModified.get( "version" );
        assertNotNull( groupId );
        assertNotNull( artifactId );
        assertNotNull( version );

        assertEquals( "org.commonjava.maven.ext.versioning.test", groupId.textValue() );
        assertEquals( "project-version", artifactId.textValue() );
        assertEquals( "1.0.0.redhat-2", version.textValue() );
    }

    private File getResourceFile( String path )
    {
        ClassLoader classLoader = getClass().getClassLoader();
        return new File( classLoader.getResource( path ).getFile() );
    }
}
