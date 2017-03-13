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
package org.commonjava.maven.ext.manip.impl;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.PomIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.BOMInjectingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.codehaus.plexus.util.StringUtils.isEmpty;

/**
 * Simple manipulator that will look for all module artifacts and construct a BOM that is deployed with the root artifact. It has a predictable naming
 * scheme making it useful in automated scenarios. Configuration consists of activation, documented in {@link BOMInjectingState}.
 */
@Component( role = Manipulator.class, hint = "bom-builder" )
public class BOMBuilderManipulator
    implements Manipulator
{
    private static final String ID = "pme-bom";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Requirement
    private PomIO pomIO;

    @Requirement ( role=Manipulator.class, hint = "profile-injection")
    private ProfileInjectionManipulator profileInjection;

    @Override
    public void init( final ManipulationSession session )
        throws ManipulationException
    {
        session.setState( new BOMInjectingState( session.getUserProperties() ) );
    }

    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * If enabled, grab the execution root pom (which will be the topmost POM in terms of directory structure). Check for the
     * presence of the build-helper-maven-plugin in the base build (/project/build/plugins/). Inject a new plugin execution for creating project
     * sources if this plugin has not already been declared in the base build section.
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final BOMInjectingState state = session.getState( BOMInjectingState.class );

        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        List<Dependency> projectArtifacts = getArtifacts( projects );

        for ( final Project project : projects )
        {
            if ( project.isExecutionRoot() )
            {
                logger.info( "Examining {} to add BOM generation.", project );

                final Model model = project.getModel();

                Build build = model.getBuild();
                if ( build == null )
                {
                    build = new Build();
                    model.setBuild( build );
                }

                final Model bomModel = new Model();
                bomModel.setModelVersion( project.getModel().getModelVersion() );
                Parent parent = new Parent();
                parent.setGroupId( model.getGroupId() );
                parent.setVersion( model.getVersion() );
                parent.setArtifactId( model.getArtifactId() );
                bomModel.setParent( parent );
                bomModel.setGroupId( model.getGroupId() + '.' + model.getArtifactId() );
                bomModel.setArtifactId( ID );
                bomModel.setPackaging( "pom" );
                bomModel.setDescription( "PME Generated BOM for other projects to use to align to." );
                DependencyManagement dm = new DependencyManagement();
                dm.setDependencies( projectArtifacts );
                bomModel.setDependencyManagement( dm );

                Profile bomProfile = new Profile();
                Activation bomActivation = new Activation();
                ActivationProperty bomActivationProperty = new ActivationProperty();
                bomActivationProperty.setName( BOMInjectingState.BOM_BUILDER );
                bomActivation.setProperty( bomActivationProperty );
                bomProfile.setActivation( bomActivation );
                bomProfile.setId( ID );
                bomProfile.addModule( ID );

                profileInjection.addProfile( model.getProfiles(), bomProfile );

                // Write it back out.
                File pmebom = new File( project.getPom().getParentFile(), ID);
                pmebom.mkdir();
                pomIO.writeModel( bomModel, new File( pmebom, "pom.xml" ) );

                return Collections.singleton( project );
            }
        }

        return Collections.emptySet();
    }


    // TODO: This will grab every module ; so those modules activated under profiles will also get included
    public List<Dependency> getArtifacts( List<Project> projects )
    {
        List<Dependency> results = new ArrayList<>(  );

        for ( Project p : projects )
        {
            Dependency d = new Dependency();
            d.setGroupId( p.getGroupId() );
            d.setArtifactId( p.getArtifactId() );
            if ( ! isEmpty ( p.getModel().getVersion() ) )
            {
                d.setVersion( p.getModel().getVersion() );
            }
            else if ( ! isEmpty ( p.getModel().getParent().getVersion() ) )
            {
                d.setVersion( p.getModel().getParent().getVersion() );
            }
            else
            {
                d.setVersion( p.getVersion() );
            }
            d.setType( p.getModel().getPackaging() );
            results.add( d );
        }
        return results;
    }

    @Override
    public int getExecutionIndex()
    {
        return 82;
    }

}
