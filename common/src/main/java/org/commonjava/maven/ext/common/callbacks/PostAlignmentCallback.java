/**
 * Copyright (C) 2019 Red Hat, Inc. (opiske@redhat.com)
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

package org.commonjava.maven.ext.common.callbacks;

import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.session.MavenSessionHandler;

import java.util.List;

public interface PostAlignmentCallback extends Callback {

    /**
     * Executes a callback after the alignment is complete
     * @param session the Maven session handler
     * @param originalProjects the original unmodified project
     * @param newProjects the new modified project
     * @throws ManipulationException
     */
    void call(MavenSessionHandler session, List<Project> originalProjects, List<Project> newProjects) throws ManipulationException;
}
