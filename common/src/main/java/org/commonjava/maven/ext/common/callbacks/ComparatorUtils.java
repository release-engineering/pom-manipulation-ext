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

import org.apache.commons.lang.StringUtils;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.model.Project;

/**
 * Utilities to compare artifacts, projects and versions.
 */
public class ComparatorUtils {

    /**
     * Checks if 2 plugin artifacts are the same
     * @param originalPVR original plugin artifact
     * @param newPVR new plugin artifact
     * @return true if equals or false otherwise
     */
    public static boolean samePluginArtifact(final ProjectVersionRef originalPVR, final ProjectVersionRef newPVR) {
        if (newPVR.getGroupId().equals(originalPVR.getGroupId()))
        {
            if (newPVR.getArtifactId().equals(originalPVR.getArtifactId()))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if 2 projects are the same
     * @param originalProject original project
     * @param newProject new project to check
     * @return true if equals or false otherwise
     */
    public static boolean sameProject(final Project originalProject, final Project newProject) {
        if (newProject.getArtifactId().equals(originalProject.getArtifactId()))
        {
            if (newProject.getGroupId().equals(originalProject.getGroupId()))
            {
                return true;
            }
        }

        return false;
    }


    /**
     * Checks if 2 artifacts are the same
     * @param originalArtifact the original artifact
     * @param newArtifact new artifact
     * @return true if equals or false otherwise
     */
    public static boolean sameArtifact(final ArtifactRef originalArtifact, final ArtifactRef newArtifact) {
        if (newArtifact.getGroupId().equals(originalArtifact.getGroupId()))
        {
            if (newArtifact.getArtifactId().equals(originalArtifact.getArtifactId()))
            {
                if (newArtifact.getType().equals(originalArtifact.getType()))
                {
                    if (StringUtils.equals(newArtifact.getClassifier(), originalArtifact.getClassifier()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Checks if the versions are the same
     * @param originalArtifact the original artifact
     * @param newArtifact new artifact
     * @param <T>
     * @return true if equals or false otherwise
     */
    public static <T extends ProjectVersionRef> boolean sameVersion(final T originalArtifact, final T newArtifact) {
        return newArtifact.getVersionString().equals( originalArtifact.getVersionString());
    }

    /**
     * Given a pair of key/values that represent the new and the old project, check if they have changed
     * @param nKey the property key from the new project
     * @param nValue the property value from the new project
     * @param oKey the property key from the old project
     * @param oValue the property value from the old project
     * @return true if they are different (ie.: changed) or false otherwise
     */
    public static boolean propertyChanged(Object nKey, Object nValue, Object oKey, Object oValue) {
        return oKey != null && oKey.equals(nKey) && oValue != null && !oValue.equals(nValue);
    }

}
