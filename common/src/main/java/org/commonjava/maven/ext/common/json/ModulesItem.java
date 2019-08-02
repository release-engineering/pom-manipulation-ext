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

package org.commonjava.maven.ext.common.json;

/*
 * Created by JacksonGenerator on 23/07/2019.
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.util.JSONUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonPropertyOrder( {"gav" } )
public class ModulesItem
{
    /**
     * Represents the new root GAV of the build.
     */
    @JsonProperty( "gav" )
    @JsonDeserialize( using = JSONUtils.ProjectVersionRefDeserializer.class )
    @JsonSerialize( using = JSONUtils.ProjectVersionRefSerializer.class )
    private ProjectVersionRef gav;

    // TODO: Complete properties
    @JsonProperty( "properties" )
    private List<PropertiesItem> properties = new ArrayList<>();

    @JsonProperty( "managedPlugins" )
    private ManagedPluginsItem managedPlugins;

    @JsonProperty( "managedDependencies" )
    private ManagedDependenciesItem managedDependencies;

    @JsonProperty( "plugins" )
    @JsonDeserialize( contentUsing = JSONUtils.ProjectVersionRefDeserializer.class )
    @JsonSerialize( contentUsing = JSONUtils.ProjectVersionRefSerializer.class )
    private Map<String, ProjectVersionRef> plugins = new HashMap<>();

    @JsonProperty( "dependencies" )
    @JsonDeserialize( contentUsing = JSONUtils.ProjectVersionRefDeserializer.class )
    @JsonSerialize( contentUsing = JSONUtils.ProjectVersionRefSerializer.class )
    private Map<String, ProjectVersionRef> dependencies = new HashMap<>();

    @JsonProperty( "profiles" )
    private List<ProfileItem> profiles = new ArrayList<>();
}
