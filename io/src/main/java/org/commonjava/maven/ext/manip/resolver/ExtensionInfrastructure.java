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
package org.commonjava.maven.ext.manip.resolver;

import java.io.File;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Settings;
import org.commonjava.maven.ext.manip.ManipulationException;

/**
 * Represents a piece of extension infrastructure that gets initialized when the {@link MavenSession} becomes available.
 * 
 * @author jdcasey
 */
public interface ExtensionInfrastructure
{
    void init( final File targetDirectory, final List<ArtifactRepository> remoteRepositories,
               final ArtifactRepository localRepository, final Settings settings, final List<String> activeProfiles)
        throws ManipulationException;

    void finish ();
}
