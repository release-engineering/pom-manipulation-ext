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

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.ModelIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.ProfileInjectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.commonjava.maven.ext.manip.util.IdUtils.ga;

/**
 * {@link Manipulator} implementation that can resolve a remote pom file and inject the remote pom's
 * profile(s) into the current project's top level pom file. Configuration is stored in a
 * {@link ProfileInjectionState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "profile-injection" )
public class ProfileInjectionManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    protected ModelIO modelBuilder;

    /**
     * No prescanning required for Profile injection.
     */
    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link ProfileInjectionState} state holder in the {@link ManipulationSession}. This state holder detects
     * version-change configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link ProfileInjectionManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new ProfileInjectionState( userProps ) );
    }

    /**
     * Apply the profile injection changes to the top level pom.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final ProfileInjectionState state = session.getState( ProfileInjectionState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<Project>();

        final Model remoteModel = modelBuilder.resolveRawModel( state.getRemoteProfileInjectionMgmt() );
        final List<Profile> remoteProfiles = remoteModel.getProfiles();

        for ( final Project project : projects )
        {
            if ( project.isInheritanceRoot())
            {
                final String ga = ga( project );
                logger.info( getClass().getSimpleName() + " applying changes to: " + ga );
                final Model model = project.getModel();
                final List<Profile> profiles = model.getProfiles();

                if ( !remoteProfiles.isEmpty() )
                {
                    final Iterator<Profile> i = remoteProfiles.iterator();
                    while ( i.hasNext() )
                    {
                        addProfile( profiles, i.next() );
                    }
                    changed.add( project );
                }
            }
        }

        return changed;
    }

    /**
     * Add the profile to the list of profiles. If an existing profile has the same
     * id it is removed first.
     *
     * @param profiles
     * @param profile
     */
    private void addProfile( final List<Profile> profiles, final Profile profile )
    {
        final Iterator<Profile> i = profiles.iterator();
        while ( i.hasNext() )
        {
            final Profile p = i.next();

            if ( profile.getId()
                        .equals( p.getId() ) )
            {
                logger.debug( "Removing local profile {} ", p );
                i.remove();
                // Don't break out of the loop so we can check for active profiles
            }

            // If we have injected profiles and one of the current profiles is using
            // activeByDefault it will get mistakingly deactivated due to the semantics
            // of activeByDefault. Therefore replace the activation.
            if (p.getActivation() != null && p.getActivation().isActiveByDefault())
            {
                logger.warn( "Profile {} is activeByDefault", p );

                final Activation replacement = new Activation();
                final ActivationProperty replacementProp = new ActivationProperty();
                replacementProp.setName( "!disableProfileActivation" );
                replacement.setProperty( replacementProp );

                p.setActivation( replacement );
            }
        }

        logger.debug( "Adding profile {}", profile );
        profiles.add( profile );
    }

    @Override
    public int getExecutionIndex()
    {
        return 45;
    }
}
