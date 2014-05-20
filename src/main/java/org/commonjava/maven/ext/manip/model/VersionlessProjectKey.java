/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.commonjava.maven.ext.manip.model;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;

public class VersionlessProjectKey
    implements Comparable<ProjectKey>, ProjectKey
{
    private final String groupId;

    private final String artifactId;

    public VersionlessProjectKey( final String groupId, final String artifactId )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public VersionlessProjectKey( final Dependency dep )
    {
        groupId = dep.getGroupId();
        artifactId = dep.getArtifactId();
    }

    public VersionlessProjectKey( final Plugin plugin )
    {
        groupId = plugin.getGroupId();
        artifactId = plugin.getArtifactId();
    }

    public VersionlessProjectKey( final Parent parent )
    {
        groupId = parent.getGroupId();
        artifactId = parent.getArtifactId();
    }

    public VersionlessProjectKey( final String ga )
    {
        String[] parts = ga.split( ":" );
        if ( parts.length < 2 )
        {
            throw new IllegalArgumentException( "Invalid versionless POM key: '" + ga + "'" );
        }

        groupId = parts[0].trim();
        artifactId = parts[1].trim();
    }

    public VersionlessProjectKey( final ReportPlugin plugin )
    {
        groupId = plugin.getGroupId();
        artifactId = plugin.getArtifactId();
    }

    public VersionlessProjectKey( final ProjectKey tk )
    {
        groupId = tk.getGroupId();
        artifactId = tk.getArtifactId();
    }

    /**
     * {@inheritDoc}
     *
     * @see com.redhat.rcm.version.model.ProjectKey#getGroupId()
     */
    @Override
    public String getGroupId()
    {
        return groupId;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.redhat.rcm.version.model.ProjectKey#getArtifactId()
     */
    @Override
    public String getArtifactId()
    {
        return artifactId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final VersionlessProjectKey other = (VersionlessProjectKey) obj;
        if ( artifactId == null )
        {
            if ( other.artifactId != null )
            {
                return false;
            }
        }
        else if ( !artifactId.equals( other.artifactId ) )
        {
            return false;
        }
        if ( groupId == null )
        {
            if ( other.groupId != null )
            {
                return false;
            }
        }
        else if ( !groupId.equals( other.groupId ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return getId();
    }

    @Override
    public int compareTo( final ProjectKey other )
    {
        if ( other == null )
        {
            return -1;
        }

        int comp = getGroupId().compareTo( other.getGroupId() );
        if ( comp == 0 )
        {
            comp = getArtifactId().compareTo( other.getArtifactId() );
        }

        return comp;
    }

    @Override
    public String getId()
    {
        return groupId + ":" + artifactId;
    }

}
