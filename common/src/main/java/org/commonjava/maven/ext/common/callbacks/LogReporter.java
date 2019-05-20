/**
 * Copyright (C) 2019 Red Hat, Inc. (opiske@redhat.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.commonjava.maven.ext.common.callbacks;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LogReporter extends PrintableReport {
    private static final Logger logger = LoggerFactory.getLogger(LogReporter.class);

    public void flush()
    {
        logger.info("------------------- project {} ", getNewProject().getKey().asProjectRef());
        if (isProjectChanged()) {
            logger.info("\tProject version : {} ---> {}\n", getOriginalProject().getVersion(),
                    getNewProject().getVersion());
        }

        printDependencies(ComparatorCallback.Type.DEPENDENCIES);
        printNonAlignedDependencies(ComparatorCallback.Type.DEPENDENCIES);
        printDependencies(ComparatorCallback.Type.MANAGED_DEPENDENCIES);
        printNonAlignedDependencies(ComparatorCallback.Type.MANAGED_DEPENDENCIES);
        pageBreak();
        printDependencies(ComparatorCallback.Type.PLUGINS);
        printNonAlignedDependencies(ComparatorCallback.Type.PLUGINS);
        printDependencies(ComparatorCallback.Type.MANAGED_PLUGINS);
        printNonAlignedDependencies(ComparatorCallback.Type.MANAGED_PLUGINS);

        getChangedProperties().forEach((k, v) ->
                logger.info("\tProperty : key {} ; value {} ---> {}", k, v.originalValue, v.newValue));
        pageBreak();
        getChangedProfileProperties().forEach((k, v) ->
                logger.info("\tProfile Property : key {} ; value {} ---> {}", k, v.originalValue, v.newValue));
        pageBreak();

        printDependencies(ComparatorCallback.Type.PROFILE_DEPENDENCIES);
        printNonAlignedDependencies(ComparatorCallback.Type.PROFILE_MANAGED_DEPENDENCIES);
        printDependencies(ComparatorCallback.Type.PROFILE_DEPENDENCIES);
        printNonAlignedDependencies(ComparatorCallback.Type.PROFILE_MANAGED_DEPENDENCIES);

    }

    private void printDependencies(ComparatorCallback.Type type) {
        List<ChangedVersion<?>> changedVersions = getChangedVersions().get(type);

        if (changedVersions != null)
        {
            changedVersions.forEach(k ->
                    logger.info("\t{} : {} --> {} ", type, k.originalArtifact, k.newArtifact));
        }
    }

    private void pageBreak() {
        logger.info("");
    }


    protected void printNonAlignedDependencies(ComparatorCallback.Type type) {
        List<? extends ProjectVersionRef> nonAlignedList = getNonAligned().get(type);

        if (nonAlignedList != null)
        {
            pageBreak();
            nonAlignedList.forEach(k -> logger.info("\tNon-Aligned {} : {} ", type, k));
        }
    }


    public void reset() {
        super.reset();
        logger.info("");
    }
}
