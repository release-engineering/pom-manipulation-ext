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
package org.commonjava.maven.ext.io.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.List;
import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public interface Translator
{
    int CHUNK_SPLIT_COUNT = 4;

    /**
     * Executes HTTP request to a REST service that translates versions
     *
     * @param projects - List of projects (GAVs)
     * @return Map of ProjectVersionRef objects as keys and translated versions as values
     * @throws RestException if an error occurs.
     */
    Map<ProjectVersionRef, String> translateVersions( List<ProjectVersionRef> projects ) throws RestException;

    List<ProjectVersionRef> findBlacklisted( ProjectRef project ) throws RestException;
}
