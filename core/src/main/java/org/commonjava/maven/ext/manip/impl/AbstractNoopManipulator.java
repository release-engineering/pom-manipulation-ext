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

package org.commonjava.maven.ext.manip.impl;

import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.model.Project;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptySet;

/**
 * Partial implementation of Manipulator interface with no-operation
 * method bodies, to avoid unnecessary empty methods in child manipulators
 * when that particular operation is not necessary.
 */
public abstract class AbstractNoopManipulator
                implements Manipulator
{

    @Override public void init( final ManipulationSession session )
                    throws ManipulationException
    {
        // noop
    }

    @Override public void scan( final List<Project> projects, final ManipulationSession session )
                    throws ManipulationException
    {
        // noop
    }

    @Override public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
                    throws ManipulationException
    {
        // noop
        return emptySet();
    }

    @Override public void afterApplyChanges( List<Project> projects, ManipulationSession session )
                    throws ManipulationException
    {
        // noop
    }

    @Override public abstract int getExecutionIndex();
}
