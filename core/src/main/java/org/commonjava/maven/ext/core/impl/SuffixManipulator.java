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
package org.commonjava.maven.ext.core.impl;

import org.apache.maven.model.Parent;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.core.ManipulationSession;
import org.commonjava.maven.ext.core.state.SuffixState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link Manipulator} implementation that can strip a version suffix. Note that this is quite similar to the
 * {@link ProjectVersioningManipulator} which can add suffixes. However this has been spawned off into a separate tool
 * so that its possible to remove the suffix before handing off to the {@link RESTCollector}.
 *
 * Configuration is stored in a {@link SuffixState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Named("suffix-manipulator")
@Singleton
public class SuffixManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ManipulationSession session;

    @Override
    public void init( final ManipulationSession session )
    {
        this.session = session;
        session.setState( new SuffixState( session.getUserProperties() ) );
    }

    /**
     * Apply the property changes to the list of {@link Project}'s given.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects )
    {
        final SuffixState state = session.getState( SuffixState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();
        final Pattern suffixStripPattern = Pattern.compile( state.getSuffixStrip() );

        for ( final Project project : projects )
        {
            final Parent parent = project.getModel().getParent();

            if ( parent != null && parent.getVersion() != null )
            {
                Matcher m = suffixStripPattern.matcher( parent.getVersion() );

                if ( m.matches() )
                {
                    logger.info( "Stripping suffix and resetting parent version from {} to {}", parent.getVersion(), m.group( 1 ) );
                    parent.setVersion( m.group(1) );
                    changed.add( project );
                }
            }
            // Not using project.getVersion as that can return the inherited parent version
            if ( project.getModel().getVersion() != null )
            {
                Matcher m = suffixStripPattern.matcher( project.getModel().getVersion() );
                if ( m.matches() )
                {
                    logger.info( "Stripping suffix and resetting project version from {} to {}", project.getModel().getVersion(), m.group( 1 ) );
                    project.getModel().setVersion( m.group(1) );
                    changed.add( project );
                }
            }
        }
        return changed;
    }

    @Override
    public int getExecutionIndex()
    {
        return 6;
    }
}
