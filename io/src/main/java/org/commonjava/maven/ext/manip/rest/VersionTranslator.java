package org.commonjava.maven.ext.manip.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public interface VersionTranslator {

    /**
     * Executes HTTP request to a REST service that translates versions
     *
     * @param projects - List of projects (GAVs)
     * @return Map of ProjectVersionRef objects as keys and translated versions as values
     */
    Map<ProjectVersionRef, String> translateVersions(List<ProjectVersionRef> projects);

}
