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

package org.commonjava.maven.ext.common.callbacks;

import org.apache.maven.model.Profile;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.common.ManipulationException;
import org.commonjava.maven.ext.common.ManipulationUncheckedException;
import org.commonjava.maven.ext.common.model.Project;
import org.commonjava.maven.ext.common.session.MavenSessionHandler;
import org.commonjava.maven.ext.common.util.ProfileUtils;

import java.util.*;
import java.util.function.BiConsumer;

import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.DEPENDENCIES;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.MANAGED_DEPENDENCIES;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.MANAGED_PLUGINS;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.PLUGINS;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.PROFILE_DEPENDENCIES;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.PROFILE_MANAGED_DEPENDENCIES;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.PROFILE_MANAGED_PLUGINS;
import static org.commonjava.maven.ext.common.callbacks.ComparatorCallback.Type.PROFILE_PLUGINS;
import static org.commonjava.maven.ext.common.callbacks.ComparatorUtils.*;

public class ComparatorCallback implements PostAlignmentCallback
{
    private static final String REPORT_NON_ALIGNED = "reportNonAligned";

    private boolean reportNonAligned;

    private final Report report;

    public ComparatorCallback(Report report) {
        this.report = report;
    }

    enum Type
    {
        DEPENDENCIES,
        MANAGED_DEPENDENCIES,
        PROFILE_DEPENDENCIES,
        PROFILE_MANAGED_DEPENDENCIES,
        PLUGINS,
        MANAGED_PLUGINS,
        PROFILE_PLUGINS,
        PROFILE_MANAGED_PLUGINS;

        @Override
        public String toString()
        {
            switch ( this )
            {
                case DEPENDENCIES:
                    return "Dependencies";
                case MANAGED_DEPENDENCIES:
                    return "Managed dependencies";
                case PROFILE_DEPENDENCIES:
                    return "Profile dependencies";
                case PROFILE_MANAGED_DEPENDENCIES:
                    return "Profile managed dependencies";
                case PLUGINS:
                    return "Plugins";
                case MANAGED_PLUGINS:
                    return "Managed plugins";
                case PROFILE_PLUGINS:
                    return "Profile plugins";
                case PROFILE_MANAGED_PLUGINS:
                    return "Profile managed plugins";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public void call(MavenSessionHandler session, List<Project> originalProjects, List<Project> newProjects) throws ManipulationException {
        reportNonAligned = Boolean.parseBoolean( session.getUserProperties().getProperty( REPORT_NON_ALIGNED, "false") );

        compareProjects(session, originalProjects, newProjects);
    }

    private void compareProjects(MavenSessionHandler session, List<Project> originalProjects, List<Project> newProjects )
                    throws ManipulationException
    {
        try
        {
            for (Project newProject : newProjects)
            {
                for (Project originalProject : originalProjects)
                {
                    if (sameProject(originalProject, newProject))
                    {
                        report.init(newProject, originalProject);

                        compareProject(session, newProject, originalProject);

                        compareProfiles(session, newProject, originalProject);

                        report.flush();

                        report.reset();
                    }
                }
            }
        }
        catch (ManipulationUncheckedException e)
        {
            throw (ManipulationException)e.getCause();
        }
    }

    private static void propertyIterator(final Properties newProperties, final Properties oldProperties,
                                  final BiConsumer<Map.Entry<Object, Object>, Map.Entry<Object, Object>> onChanged) {
        for (Map.Entry<Object, Object> newEntry : newProperties.entrySet())
        {
            Object nKey = newEntry.getKey();
            Object nValue = newEntry.getValue();

            for (Map.Entry<Object, Object> oldEntry : oldProperties.entrySet())
             {
                Object oKey = oldEntry.getKey();
                Object oValue = oldEntry.getValue();

                if (propertyChanged(nKey, nValue, oKey, oValue)) {
                    onChanged.accept(newEntry, oldEntry);
                }
            }
        }
    }

    private void compareProject(MavenSessionHandler session, Project newProject, Project originalProject) throws ManipulationException {


        if ( ! originalProject.getVersion().equals( newProject.getVersion() ) )
        {
            report.projectVersionChanged();
        }

        propertyIterator(newProject.getModel().getProperties(), originalProject.getModel().getProperties(),
                report::propertyChanged);

        compareDependencies( DEPENDENCIES,
                             handleDependencies( session, originalProject, null, DEPENDENCIES ),
                             handleDependencies( session, newProject, null, DEPENDENCIES ) );

        compareDependencies( MANAGED_DEPENDENCIES,
                             handleDependencies( session, originalProject, null,
                                                 MANAGED_DEPENDENCIES ),
                             handleDependencies( session, newProject, null, MANAGED_DEPENDENCIES ) );

        comparePlugins( PLUGINS,
                             handlePlugins( session, originalProject, null, PLUGINS ),
                             handlePlugins( session, newProject, null, PLUGINS ) );
        comparePlugins( MANAGED_PLUGINS,
                        handlePlugins( session, originalProject, null,
                                       MANAGED_PLUGINS ),
                        handlePlugins( session, newProject, null, MANAGED_PLUGINS ) );
    }



    private void compareProfiles(MavenSessionHandler session, Project newProject, Project originalProject) {
        List<Profile> oldProfiles = ProfileUtils.getProfiles( session, originalProject.getModel() );
        List<Profile> newProfiles = ProfileUtils.getProfiles( session, newProject.getModel() );

        for (Profile newProfile : newProfiles)
        {
            for (Profile oldProfile : oldProfiles)
            {
                if (newProfile.getId().equals(oldProfile.getId()))
                {
                    propertyIterator(newProfile.getProperties(), oldProfile.getProperties(),
                            report::profilePropertyChanged);

                    compareDependencies(PROFILE_DEPENDENCIES,
                            handleDependencies(session, originalProject, oldProfile, PROFILE_DEPENDENCIES),
                            handleDependencies(session, newProject, newProfile, PROFILE_DEPENDENCIES));

                    compareDependencies(PROFILE_MANAGED_DEPENDENCIES,
                            handleDependencies(session, originalProject,
                                    oldProfile,
                                    PROFILE_MANAGED_DEPENDENCIES),
                            handleDependencies(session, newProject, newProfile,
                                    PROFILE_MANAGED_DEPENDENCIES));

                    comparePlugins(PROFILE_PLUGINS,
                            handlePlugins(session, originalProject, oldProfile, PROFILE_PLUGINS),
                            handlePlugins(session, newProject, newProfile, PROFILE_PLUGINS));

                    comparePlugins(PROFILE_MANAGED_PLUGINS,
                            handlePlugins(session, originalProject,
                                    oldProfile,
                                    PROFILE_MANAGED_PLUGINS),
                            handlePlugins(session, newProject, newProfile,
                                    PROFILE_MANAGED_PLUGINS));
                }
            }
        }
    }


    private void compareDependencies( Type type, Set<ArtifactRef> originalDeps, Set<ArtifactRef> newDeps )
    {
        for (ArtifactRef originalArtifact : originalDeps)
        {
            for (ArtifactRef newArtifact : newDeps)
            {
                if (sameArtifact(originalArtifact, newArtifact))
                {
                    if (!sameVersion(originalArtifact, newArtifact))
                    {
                        report.reportVersionChanged(type, originalArtifact, newArtifact);

                        if (reportNonAligned)
                        {
                            report.reportNonAligned(type, originalArtifact);
                        }
                    }
                }
            }
        }
    }



    private void comparePlugins( Type type, Set<ProjectVersionRef> originalPlugins, Set<ProjectVersionRef> newPlugins )
    {
        for (ProjectVersionRef originalPVR : originalPlugins)
        {
            for (ProjectVersionRef newPVR : newPlugins)
            {
                if (samePluginArtifact(originalPVR, newPVR))
                {
                    if (!sameVersion(originalPVR, newPVR))
                    {
                        report.reportVersionChanged(type, originalPVR, newPVR);

                        if (reportNonAligned)
                        {
                            report.reportNonAligned(type, originalPVR);
                        }
                    }
                }
            }
        }


    }


    private static Set<ArtifactRef> handleDependencies( MavenSessionHandler session, Project project, Profile profile,
                                                        Type type )
                    throws ManipulationUncheckedException
    {
        try
        {
            switch (type)
            {
                case DEPENDENCIES:
                {
                    return project.getResolvedDependencies( session ).keySet();
                }
                case MANAGED_DEPENDENCIES:
                {
                    return project.getResolvedManagedDependencies( session ).keySet();
                }
                case PROFILE_DEPENDENCIES:
                {
                    return project.getResolvedProfileDependencies( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                case PROFILE_MANAGED_DEPENDENCIES:
                {
                    return project.getResolvedProfileManagedDependencies( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                default:
                {
                    throw new ManipulationException( "Invalid type " + type.toString() );
                }
            }
        }
        catch ( ManipulationException e )
        {
            throw new ManipulationUncheckedException( e );
        }
    }

    private static Set<ProjectVersionRef> handlePlugins( MavenSessionHandler session, Project project, Profile profile,
                                                        Type type )
                    throws ManipulationUncheckedException
    {
        try
        {
            switch (type)
            {
                case PLUGINS:
                {
                    return project.getResolvedPlugins( session ).keySet();
                }
                case MANAGED_PLUGINS:
                {
                    return project.getResolvedManagedPlugins( session ).keySet();
                }
                case PROFILE_PLUGINS:
                {
                    return project.getResolvedProfilePlugins( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                case PROFILE_MANAGED_PLUGINS:
                {
                    return project.getResolvedProfileManagedPlugins( session ).getOrDefault( profile, Collections.emptyMap() ).keySet();
                }
                default:
                {
                    throw new ManipulationException( "Invalid type " + type.toString() );
                }
            }
        }
        catch ( ManipulationException e )
        {
            throw new ManipulationUncheckedException( e );
        }
    }
}
