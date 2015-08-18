package org.commonjava.maven.ext.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.List;

/**
 * @author vdedik@redhat.com
 */
public interface VersionTranslator {

    /**
     * Executes HTTP request to a REST service that translates versions
     *
     * @param project - Represents project with groupId, artifactId and version
     * @param dependencies - List of dependencies of the project
     * @return List of ProjectVersionRef objects, cointains both the main project and it's dependencies
     */
    List<ProjectVersionRef> translateVersions(ProjectVersionRef project, List<ProjectVersionRef> dependencies);

}
