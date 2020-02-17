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

package org.commonjava.maven.ext.common.model;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.InputLocationTracker;
import org.apache.maven.model.Plugin;
import org.commonjava.maven.ext.common.ManipulationException;

/**
 * Simple wrapper to allow a Plugin or Dependency to be handled within the same way.
 */
public class DependencyPluginWrapper
{
    private Plugin plugin = null;

    private Dependency dependency = null;

    public DependencyPluginWrapper( InputLocationTracker o ) throws ManipulationException
    {
        if ( o instanceof Dependency)
        {
            dependency = (Dependency)o;
        }
        else if (o instanceof Plugin)
        {
            plugin = (Plugin)o;
        }
        else
        {
            throw new ManipulationException( "Unknown type for wrapper {}", o );
        }
    }

    public String getVersion()
    {
        if ( dependency != null )
        {
            return dependency.getVersion();
        }
        else
        {
            return plugin.getVersion();
        }
    }

    public void setVersion( String target )
    {
        if ( dependency != null )
        {
            dependency.setVersion( target );
        }
        else
        {
            plugin.setVersion( target );
        }

    }

    public void addExclusion( Exclusion e ) throws ManipulationException
    {
        if ( dependency == null )
        {
            throw new ManipulationException( "Type is not a dependency {}", e);
        }
        dependency.addExclusion(e);
    }
}
