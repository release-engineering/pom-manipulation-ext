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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

public class FullProjectKey
    extends VersionlessProjectKey
{

    private final String version;

    private Dependency bomDep;

    public FullProjectKey( final String groupId, final String artifactId, final String version )
    {
        super( groupId, artifactId );
        this.version = version;
    }

    public FullProjectKey( final ProjectKey key, final String version )
    {
        this( key.getGroupId(), key.getArtifactId(), version );
    }

    public FullProjectKey( final Parent parent )
    {
        this( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
    }

    public FullProjectKey( final Dependency dependency )
    {
        this( dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion() );
    }

    public FullProjectKey( final Model model )
    {
        this( selectGroupId( model ), model.getArtifactId(), selectVersion( model ) );
    }

    public FullProjectKey( final MavenProject project )
    {
        this( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    private static String selectVersion( final Model model )
    {
        String version = model.getVersion();
        if ( version == null )
        {
            Parent parent = model.getParent();
            if ( parent != null )
            {
                version = parent.getVersion();
            }
        }

        if ( version == null )
        {
            throw new IllegalArgumentException( "Invalid model (missing version): " + model );
        }

        return version;
    }

    private static String selectGroupId( final Model model )
    {
        String gid = model.getGroupId();
        if ( gid == null )
        {
            Parent parent = model.getParent();
            if ( parent != null )
            {
                gid = parent.getGroupId();
            }
        }

        if ( gid == null )
        {
            throw new IllegalArgumentException( "Invalid model (missing groupId): " + model );
        }

        return gid;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( !super.equals( obj ) )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final FullProjectKey other = (FullProjectKey) obj;
        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return super.getId() + ":" + version;
    }

    public synchronized Dependency getBomDependency()
    {
        if ( bomDep == null )
        {
            bomDep = new Dependency();
            bomDep.setGroupId( getGroupId() );
            bomDep.setArtifactId( getArtifactId() );
            bomDep.setVersion( version );
            bomDep.setType( "pom" );
            bomDep.setScope( Artifact.SCOPE_IMPORT );
        }

        return bomDep;
    }

}
