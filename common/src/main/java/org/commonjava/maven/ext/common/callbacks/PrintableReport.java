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

package org.commonjava.maven.ext.common.callbacks;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class PrintableReport implements Report {
    private Project newProject;
    private Project originalProject;
    private boolean projectChanged = false;
    private HashMap<Object, ChangedProperty> changedProperties = new HashMap<>();
    private HashMap<Object, ChangedProperty> changedProfileProperties = new HashMap<>();
    private HashMap<ComparatorCallback.Type, List<ChangedVersion<?>>> changedVersions = new HashMap<>();
    private HashMap<ComparatorCallback.Type, List<? extends ProjectVersionRef>> nonAligned = new HashMap<>();

    class ChangedProperty {
        Object originalValue;
        Object newValue;

        public ChangedProperty(Object originalValue, Object newValue) {
            this.originalValue = originalValue;
            this.newValue = newValue;
        }
    }

    class ChangedVersion<T> {
        T originalArtifact;
        T newArtifact;

        public ChangedVersion(T originalArtifact, T newArtifact) {
            this.originalArtifact = originalArtifact;
            this.newArtifact = newArtifact;
        }
    }

    public void init(final Project newProject, final Project originalProject) {

        this.newProject = newProject;
        this.originalProject = originalProject;
    }

    public abstract void flush() throws ManipulationException;

    public void projectVersionChanged() {
        projectChanged = true;
    }

    private void propertyChanged(final Object nValue, final Object oKey, final Object oValue) {
        changedProperties.put(oKey, new ChangedProperty(oValue, nValue));
    }

    public void propertyChanged(final Map.Entry<Object, Object> newProperty, final Map.Entry<Object, Object> oldProperty) {
        propertyChanged(newProperty.getValue(), oldProperty.getKey(), oldProperty.getValue());
    }

    private void profilePropertyChanged(final Object nValue, final Object oKey, final Object oValue) {
        changedProfileProperties.put(oKey, new ChangedProperty(oValue, nValue));

    }

    public void profilePropertyChanged(final Map.Entry<Object, Object> newProperty, final Map.Entry<Object, Object> oldProperty) {
        profilePropertyChanged(newProperty.getValue(), oldProperty.getKey(), oldProperty.getValue());
    }

    public <T extends ProjectVersionRef> void reportVersionChanged(ComparatorCallback.Type type, T originalArtifact, T newArtifact) {
        List<ChangedVersion<?>> artifactsList = changedVersions.get(type);
        if (artifactsList == null) {
            artifactsList = new ArrayList<>();
        }

        artifactsList.add(new ChangedVersion<T>(originalArtifact, newArtifact));
        changedVersions.put(type, artifactsList);
    }

    public <T extends ProjectVersionRef> void reportNonAligned(ComparatorCallback.Type type, T nonAlignedArtifact) {
        List<T> nonAlignedSet = (List<T>) nonAligned.get(type);

        if (nonAlignedSet == null) {
            nonAlignedSet = new ArrayList<>();
        }

        nonAlignedSet.add(nonAlignedArtifact);

        nonAligned.put(type, nonAlignedSet);
    }

    protected Project getNewProject() {
        return newProject;
    }

    protected Project getOriginalProject() {
        return originalProject;
    }

    protected boolean isProjectChanged() {
        return projectChanged;
    }

    protected HashMap<Object, ChangedProperty> getChangedProperties() {
        return changedProperties;
    }

    protected HashMap<Object, ChangedProperty> getChangedProfileProperties() {
        return changedProfileProperties;
    }

    protected HashMap<ComparatorCallback.Type, List<ChangedVersion<?>>> getChangedVersions() {
        return changedVersions;
    }

    protected HashMap<ComparatorCallback.Type, List<? extends ProjectVersionRef>> getNonAligned() {
        return nonAligned;
    }

    public void reset() {
        newProject = null;
        originalProject = null;
        projectChanged = false;
        changedProperties.clear();
        changedProfileProperties.clear();
        changedVersions.clear();
        nonAligned.clear();
    }
}
