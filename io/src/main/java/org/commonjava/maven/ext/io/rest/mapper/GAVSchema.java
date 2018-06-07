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
package org.commonjava.maven.ext.io.rest.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GAVSchema
{
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String versionSuffix;

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String[] productNames;

    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String[] productVersionIds;

    // Backwards compatibility for versions of DependencyAnalyser that don't have this field.
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    public String repositoryGroup;

    public List<Map<String, Object>> gavs;

    public GAVSchema() {}

    public GAVSchema( String[] productNames, String[] productVersionIds, String repositoryGroup, String versionSuffix,
                        List<Map<String, Object>> gavs )
    {
        this.productNames = productNames;
        this.productVersionIds = productVersionIds;
        this.repositoryGroup = repositoryGroup;
        this.gavs = gavs;
        this.versionSuffix = versionSuffix;
    }

    @Override
    public String toString()
    {
        return "ProductNames '" + Arrays.toString( productNames ) +
                        "' :: ProductVersionIds '" + Arrays.toString( productVersionIds )+
                        "' :: RepositoryGroup '" + repositoryGroup +
                        "' :: versionSuffix '" + versionSuffix +
                        "' :: gavs " + gavs;
    }
}
