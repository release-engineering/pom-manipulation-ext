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
package org.commonjava.maven.ext.manip.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.state.State;
import org.commonjava.maven.ext.manip.util.IdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Manipulator} base implementation used by the dependency and plugin manipulators.
 */
public abstract class AlignmentManipulator
    implements Manipulator
{
    protected enum RemoteType
    {
        PLUGIN, DEPENDENCY
    };

    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected ModelIO effectiveModelBuilder;

    protected AlignmentManipulator()
    {
    }

    protected AlignmentManipulator( final ModelIO modelIO )
    {
        this.effectiveModelBuilder = modelIO;
    }

    /**
     * No prescanning required for BOM manipulation.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Generic applyChanges shared between Plugin and Dependency manipulators
     */
    protected Set<Project> internalApplyChanges( final State state, final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Map<ProjectRef, String> overrides = loadRemoteBOM( state, session );
        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final Model model = project.getModel();

            if ( overrides.size() > 0 )
            {
                apply( session, project, model, overrides );

                changed.add( project );
            }
        }

        return changed;
    }

    /**
     * Get property mappings from a remote POM
     *
     * @return Map between the GA of the plugin and the version of the plugin. If the system property is not set,
     *         returns an empty map.
     */
    protected Map<ProjectRef, String> loadRemoteOverrides( final RemoteType rt, final String remoteMgmt,
                                                       final ManipulationSession session )
        throws ManipulationException
    {
        final Map<ProjectRef, String> overrides = new LinkedHashMap<ProjectRef, String>();

        if ( remoteMgmt == null || remoteMgmt.length() == 0 )
        {
            return overrides;
        }

        final String[] remoteMgmtPomGAVs = remoteMgmt.split( "," );

        // Iterate in reverse order so that the first GAV in the list overwrites the last
        for ( int i = ( remoteMgmtPomGAVs.length - 1 ); i > -1; --i )
        {
            final String nextGAV = remoteMgmtPomGAVs[i];

            if ( !IdUtils.validGav( nextGAV ) )
            {
                logger.warn( "Skipping invalid remote management GAV: " + nextGAV );
                continue;
            }
            switch ( rt )
            {
                case PLUGIN:
                    overrides.putAll( effectiveModelBuilder.getRemotePluginVersionOverrides( nextGAV, session ) );
                    break;
                case DEPENDENCY:
                    overrides.putAll( effectiveModelBuilder.getRemoteDependencyVersionOverrides( nextGAV, session ) );
                    break;

            }
        }

        return overrides;
    }

    /**
     * Abstract method to be implemented by subclasses. Returns the remote bom.
     * @param state
     * @param session TODO
     * @param project TODO
     * @param model
     * @param override
     * @throws ManipulationException
     */
    protected abstract Map<ProjectRef, String> loadRemoteBOM( State state, ManipulationSession session )
        throws ManipulationException;

    /**
     * Abstract method to be implemented by subclasses. Performs the actual injection on the pom file.
     * @param session TODO
     * @param project TODO
     * @param model
     * @param overrideloadRemoteBOM( state, session )
     * @throws ManipulationException TODO
     */
    protected abstract void apply( ManipulationSession session, Project project, Model model,
                                   Map<ProjectRef, String> override )
        throws ManipulationException;
}
