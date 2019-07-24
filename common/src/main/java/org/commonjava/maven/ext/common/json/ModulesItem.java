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

package org.commonjava.maven.ext.common.json;

/**
 * Created by JacksonGenerator on 23/07/2019.
 */

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import java.util.ArrayList;
import java.util.List;

@Data
public class ModulesItem
{
    @JsonProperty( "gav" )
    private GAV gav;

    @JsonProperty( "managedPlugins" )
    private List<ManagedPluginsItem> managedPlugins = new ArrayList<>();

    @JsonProperty( "managedDependencies" )
    private List<ManagedDependenciesItem> managedDependencies = new ArrayList<>();

    @JsonProperty( "plugins" )
    private List<PluginsItem> plugins = new ArrayList<>();

    @JsonProperty( "profiles" )
    private List<ProfilesItem> profiles = new ArrayList<>();

    @JsonProperty( "properties" )
    private List<PropertiesItem> properties = new ArrayList<>();

    @JsonProperty( "dependencies" )
    private List<DependenciesItem> dependencies = new ArrayList<>();

    public ModulesItem( ProjectVersionRef newPVR, String oldVersion )
    {
        gav = new GAV( newPVR );
        gav.setOldVersion( oldVersion );
    }
}