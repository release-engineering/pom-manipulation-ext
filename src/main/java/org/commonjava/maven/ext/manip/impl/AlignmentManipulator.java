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

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.resolver.ModelOverridesResolver;
import org.commonjava.maven.ext.manip.state.BOMState;
import org.commonjava.maven.ext.manip.state.ManipulationSession;
import org.commonjava.maven.ext.manip.util.IdUtils;

/**
 * {@link Manipulator} base implementation used by the property, dependency and plugin manipulators.
 * Configuration is stored in a {@link BOMState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
public abstract class AlignmentManipulator
    implements Manipulator
{
    protected enum RemoteType
    {
        PROPERTY, PLUGIN, DEPENDENCY
    };

    @Requirement
    protected Logger logger;

    @Requirement
    protected ModelOverridesResolver effectiveModelBuilder;

    protected AlignmentManipulator()
    {
    }

    public AlignmentManipulator( final Logger logger )
    {
        this.logger = logger;
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
     * Initialize the {@link BOMState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link AlignmentManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new BOMState( userProps ) );
    }

    /**
     * Apply the reporting and repository removal changes to the list of {@link MavenProject}'s given.
     * This happens near the end of the Maven session-bootstrapping sequence, before the projects are
     * discovered/read by the main Maven build initialization.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final BOMState state = session.getState( BOMState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( "Alignment Manipulator: Nothing to do!" );
            return Collections.emptySet();
        }

        final Map<String, Model> manipulatedModels = session.getManipulatedModels();
        final Map<String, String> overrides = loadRemoteBOM( state, session );
        final Set<Project> changed = new HashSet<Project>();

        for ( final Project project : projects )
        {
            final String ga = ga( project );
            final Model model = manipulatedModels.get( ga );

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
    protected Map<String, String> loadRemoteOverrides( final RemoteType rt, final String remoteMgmt,
                                                       final ManipulationSession session )
        throws ManipulationException
    {
        final Map<String, String> overrides = new HashMap<String, String>();

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
                case PROPERTY:
                    overrides.putAll( effectiveModelBuilder.getRemotePropertyMappingOverrides( nextGAV, session ) );
                    break;
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
    protected abstract Map<String, String> loadRemoteBOM( BOMState state, ManipulationSession session )
        throws ManipulationException;

    /**
     * Abstract method to be implemented by subclasses. Performs the actual injection on the pom file.
     * @param session TODO
     * @param project TODO
     * @param model
     * @param override
     * @throws ManipulationException TODO
     */
    protected abstract void apply( ManipulationSession session, Project project, Model model,
                                   Map<String, String> override )
        throws ManipulationException;
}
