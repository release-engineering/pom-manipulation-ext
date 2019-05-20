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

import org.apache.commons.io.FileUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.model.Project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;

public class FileReporter extends PrintableReport {
    private static final String ALIGNED_DEP_EXT = ".dependencies";
    private static final String NON_ALIGNED_DEP_EXT = ".na-dependencies";

    private File outputDir;

    public FileReporter(String outputDir) {
        this(new File(outputDir));
    }

    public FileReporter(File outputDir) {
        this.outputDir = outputDir;

        outputDir.mkdirs();
    }

    private String getFormattedGAV(Project originalProject) {
        return originalProject.getGroupId() + ":" + originalProject.getArtifactId() + ":" + originalProject.getVersion();
    }

    private void saveUpdatedProjectInformation() throws ManipulationException {
        File outputFile = new File(outputDir, getNewProject().getArtifactId() + ".version");

        final String origProj = getFormattedGAV(getOriginalProject());
        final String newProj = getFormattedGAV(getNewProject());

        try
        {
            FileUtils.writeStringToFile(outputFile, String.format("%s=%s", origProj, newProj), Charset.defaultCharset());
        } catch (IOException e)
        {
            throw new ManipulationException("Unable to save report", e);
        }
    }

    private void printDependencies(ComparatorCallback.Type type, StringBuffer sb) {
        List<ChangedVersion<?>> changedVersions = getChangedVersions().get(type);

        if (changedVersions != null)
        {
            changedVersions.forEach(k ->
                    sb.append(String.format("%s;%s;%s\n", type, k.originalArtifact, k.newArtifact)));
        }
    }

    private void printNonAlignedDependencies(ComparatorCallback.Type type, StringBuffer sb) {
        List<? extends ProjectVersionRef> changedVersions = getNonAligned().get(type);

        if (changedVersions != null)
        {
            changedVersions.forEach(k ->
                    sb.append(String.format("%s;%s\n", type, k)));
        }
    }

    private void saveDependencies(String extension, BiConsumer<ComparatorCallback.Type, StringBuffer> printer) throws ManipulationException {
        StringBuffer sb = new StringBuffer();

        try
        {
            File outputFile = new File(outputDir, getNewProject().getArtifactId() + extension);

            printer.accept(ComparatorCallback.Type.DEPENDENCIES, sb);
            printer.accept(ComparatorCallback.Type.MANAGED_DEPENDENCIES, sb);
            printer.accept(ComparatorCallback.Type.PLUGINS, sb);
            printer.accept(ComparatorCallback.Type.MANAGED_PLUGINS, sb);
            printer.accept(ComparatorCallback.Type.PROFILE_DEPENDENCIES, sb);
            printer.accept(ComparatorCallback.Type.PROFILE_MANAGED_DEPENDENCIES, sb);

            FileUtils.writeStringToFile(outputFile, sb.toString(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new ManipulationException("Unable to save report", e);
        }
    }


    @Override
    public void flush() throws ManipulationException {
        saveUpdatedProjectInformation();
        saveDependencies(ALIGNED_DEP_EXT, this::printDependencies);
        saveDependencies(NON_ALIGNED_DEP_EXT, this::printNonAlignedDependencies);
    }
}
