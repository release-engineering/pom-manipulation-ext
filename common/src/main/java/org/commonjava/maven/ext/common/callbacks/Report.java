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

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;

import java.util.Map;
import java.util.Set;

/**
 * An interface that allows the implementation of reports based on the comparison of projects
 */
public interface Report {
    /**
     * Initializes the report
     * @param newProject the new project
     */
    void init(final Project newProject, final Project originalProject);

    /**
     * Reports a project version change
     */
    void projectVersionChanged() throws ManipulationException;

    /**
     * Reports a property change
     * @param newProperty the new property
     * @param oldProperty the original (old) property
     */
    void propertyChanged(Map.Entry<Object, Object> newProperty, Map.Entry<Object, Object> oldProperty);

    /**
     * Reports property changed within a profile
     * @param newProperty the new property
     * @param oldProperty the original (old) property
     */
    void profilePropertyChanged(Map.Entry<Object, Object> newProperty, Map.Entry<Object, Object> oldProperty);

    /**
     * Reports a version change
     * @param type artifact type
     * @param originalArtifact original artifact
     * @param newArtifact new artifact
     * @param <T>
     */
    <T extends ProjectVersionRef> void reportVersionChanged(ComparatorCallback.Type type, T originalArtifact, T newArtifact);


    /**
     * Reports non-aligned dependencies
     * @param type
     * @param nonAligned
     * @param <T>
     */
    <T extends ProjectVersionRef> void reportNonAligned(ComparatorCallback.Type type, T nonAligned);


    /**
     * Flushes the report
     */
    void flush() throws ManipulationException;

    /**
     * Reset
     */
    void reset();
}
