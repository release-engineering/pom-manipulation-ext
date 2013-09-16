package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.gav;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.DefaultModelWriter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;

public class VersioningModifierTest
{

    @Test
    public void updateEffectiveAndOriginalModelMainVersions()
        throws Exception
    {
        final Model orig = new Model();
        orig.setGroupId( "org.foo" );
        orig.setArtifactId( "bar" );
        orig.setVersion( "1.0" );

        final Model eff = orig.clone();

        final String suff = "redhat-1";
        final String mv = orig.getVersion() + "." + suff;

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        versionsByGA.put( gav( orig.getGroupId(), orig.getArtifactId(), orig.getVersion() ), mv );

        final MavenProject project = new MavenProject( eff );
        project.setOriginalModel( orig );

        final Set<MavenProject> changes =
            newVersioningModifier().applyVersioningChanges( Collections.singleton( project ), versionsByGA );

        assertThat( changes.size(), equalTo( 1 ) );
        assertThat( orig.getVersion(), equalTo( mv ) );
        assertThat( eff.getVersion(), equalTo( mv ) );
    }

    @Test
    public void updateEffectiveAndOriginalModelParentVersions()
        throws Exception
    {
        final Model parent = new Model();
        parent.setGroupId( "org.foo" );
        parent.setArtifactId( "bar-parent" );
        parent.setVersion( "1.0" );

        final Model orig = new Model();
        orig.setArtifactId( "bar" );

        final Parent origParent = new Parent();
        origParent.setGroupId( parent.getGroupId() );
        origParent.setArtifactId( parent.getArtifactId() );
        origParent.setVersion( parent.getVersion() );

        orig.setParent( origParent );

        final Model eff = orig.clone();

        final String suff = "redhat-1";
        final String mv = orig.getVersion() + "." + suff;

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        versionsByGA.put( gav( orig.getGroupId(), orig.getArtifactId(), orig.getVersion() ), mv );
        versionsByGA.put( gav( origParent.getGroupId(), origParent.getArtifactId(), origParent.getVersion() ), mv );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        MavenProject project = new MavenProject( parent.clone() );
        project.setOriginalModel( parent );
        projects.add( project );

        project = new MavenProject( eff );
        project.setOriginalModel( orig );
        projects.add( project );

        final Set<MavenProject> changes = newVersioningModifier().applyVersioningChanges( projects, versionsByGA );

        assertThat( changes.size(), equalTo( 2 ) );
        for ( final MavenProject p : changes )
        {
            if ( p.getArtifactId()
                  .equals( "bar" ) )
            {
                assertThat( p.getOriginalModel()
                             .getParent()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getModel()
                             .getParent()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getOriginalModel()
                             .getVersion(), nullValue() );
                assertThat( p.getModel()
                             .getVersion(), nullValue() );
            }
            else
            {
                assertThat( p.getOriginalModel()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getModel()
                             .getVersion(), equalTo( mv ) );
            }
        }
    }

    @Test
    public void updateEffectiveAndOriginalModelDependencyVersions()
        throws Exception
    {
        final Model orig = new Model();
        orig.setGroupId( "org.foo" );
        orig.setArtifactId( "bar" );
        orig.setVersion( "1.0" );

        final Model depModel = new Model();
        depModel.setGroupId( "org.foo" );
        depModel.setArtifactId( "bar-dep" );
        depModel.setVersion( "1.0" );

        final Dependency dep = new Dependency();
        dep.setGroupId( depModel.getGroupId() );
        dep.setArtifactId( depModel.getArtifactId() );
        dep.setVersion( depModel.getVersion() );

        orig.addDependency( dep );

        final DependencyManagement mgmt = new DependencyManagement();

        final Model dmModel = new Model();
        dmModel.setGroupId( "org.foo" );
        dmModel.setArtifactId( "bar-managed-dep" );
        dmModel.setVersion( "1.0" );

        final Dependency managed = new Dependency();
        managed.setGroupId( dmModel.getGroupId() );
        managed.setArtifactId( dmModel.getArtifactId() );
        managed.setVersion( dmModel.getVersion() );

        mgmt.addDependency( managed );
        orig.setDependencyManagement( mgmt );

        final String suff = "redhat-1";
        final String mv = orig.getVersion() + "." + suff;

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        versionsByGA.put( gav( orig.getGroupId(), orig.getArtifactId(), orig.getVersion() ), mv );
        versionsByGA.put( gav( depModel.getGroupId(), depModel.getArtifactId(), depModel.getVersion() ), mv );
        versionsByGA.put( gav( dmModel.getGroupId(), dmModel.getArtifactId(), dmModel.getVersion() ), mv );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        MavenProject project = new MavenProject( depModel.clone() );
        project.setOriginalModel( depModel );
        projects.add( project );

        project = new MavenProject( dmModel.clone() );
        project.setOriginalModel( dmModel );
        projects.add( project );

        project = new MavenProject( orig.clone() );
        project.setOriginalModel( orig );
        projects.add( project );

        final Set<MavenProject> changes = newVersioningModifier().applyVersioningChanges( projects, versionsByGA );

        assertThat( changes.size(), equalTo( 3 ) );
        for ( final MavenProject p : changes )
        {
            final String a = p.getArtifactId();

            if ( a.equals( "bar" ) )
            {
                assertThat( p.getOriginalModel()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getModel()
                             .getVersion(), equalTo( mv ) );

                List<Dependency> deps = p.getOriginalModel()
                                         .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                Dependency d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = p.getModel()
                        .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = p.getOriginalModel()
                        .getDependencyManagement()
                        .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = p.getModel()
                        .getDependencyManagement()
                        .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

            }
            else
            {
                assertThat( p.getOriginalModel()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getModel()
                             .getVersion(), equalTo( mv ) );
            }
        }
    }

    @Test
    public void updateEffectiveAndOriginalModelDependencyVersions_OnlyWhenHasVersion()
        throws Exception
    {
        final Model orig = new Model();
        orig.setGroupId( "org.foo" );
        orig.setArtifactId( "bar" );
        orig.setVersion( "1.0" );

        final Model depModel = new Model();
        depModel.setGroupId( "org.foo" );
        depModel.setArtifactId( "bar-dep" );
        depModel.setVersion( "1.0" );

        final Dependency dep = new Dependency();
        dep.setGroupId( depModel.getGroupId() );
        dep.setArtifactId( depModel.getArtifactId() );

        orig.addDependency( dep );

        final DependencyManagement mgmt = new DependencyManagement();

        final Dependency managed = new Dependency();
        managed.setGroupId( depModel.getGroupId() );
        managed.setArtifactId( depModel.getArtifactId() );
        managed.setVersion( depModel.getVersion() );

        mgmt.addDependency( managed );
        orig.setDependencyManagement( mgmt );

        final String suff = "redhat-1";
        final String mv = orig.getVersion() + "." + suff;

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        versionsByGA.put( gav( orig.getGroupId(), orig.getArtifactId(), orig.getVersion() ), mv );
        versionsByGA.put( gav( depModel.getGroupId(), depModel.getArtifactId(), depModel.getVersion() ), mv );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        MavenProject project = new MavenProject( depModel.clone() );
        project.setOriginalModel( depModel );
        projects.add( project );

        project = new MavenProject( orig.clone() );
        project.setOriginalModel( orig );
        projects.add( project );

        final Set<MavenProject> changes = newVersioningModifier().applyVersioningChanges( projects, versionsByGA );

        assertThat( changes.size(), equalTo( 2 ) );
        for ( final MavenProject p : changes )
        {
            final String a = p.getArtifactId();

            if ( a.equals( "bar" ) )
            {
                assertThat( p.getOriginalModel()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getModel()
                             .getVersion(), equalTo( mv ) );

                List<Dependency> deps = p.getOriginalModel()
                                         .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                Dependency d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), nullValue() );

                deps = p.getModel()
                        .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), nullValue() );

                deps = p.getOriginalModel()
                        .getDependencyManagement()
                        .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = p.getModel()
                        .getDependencyManagement()
                        .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

            }
            else
            {
                assertThat( p.getOriginalModel()
                             .getVersion(), equalTo( mv ) );
                assertThat( p.getModel()
                             .getVersion(), equalTo( mv ) );
            }
        }
    }

    @Test
    public void updateEffectiveAndOriginalModelDependencyVersions_InProfile()
        throws Exception
    {
        final Model orig = new Model();
        orig.setGroupId( "org.foo" );
        orig.setArtifactId( "bar" );
        orig.setVersion( "1.0" );

        final Model depModel = new Model();
        depModel.setGroupId( "org.foo" );
        depModel.setArtifactId( "bar-dep" );
        depModel.setVersion( "1.0" );

        final Dependency dep = new Dependency();
        dep.setGroupId( depModel.getGroupId() );
        dep.setArtifactId( depModel.getArtifactId() );
        dep.setVersion( depModel.getVersion() );

        final Profile p = new Profile();
        p.setId( "test" );
        orig.addProfile( p );

        p.addDependency( dep );

        final DependencyManagement mgmt = new DependencyManagement();

        final Model dmModel = new Model();
        dmModel.setGroupId( "org.foo" );
        dmModel.setArtifactId( "bar-managed-dep" );
        dmModel.setVersion( "1.0" );

        final Dependency managed = new Dependency();
        managed.setGroupId( dmModel.getGroupId() );
        managed.setArtifactId( dmModel.getArtifactId() );
        managed.setVersion( dmModel.getVersion() );

        mgmt.addDependency( managed );
        p.setDependencyManagement( mgmt );

        final String suff = "redhat-1";
        final String mv = orig.getVersion() + "." + suff;

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        versionsByGA.put( gav( orig.getGroupId(), orig.getArtifactId(), orig.getVersion() ), mv );
        versionsByGA.put( gav( depModel.getGroupId(), depModel.getArtifactId(), depModel.getVersion() ), mv );
        versionsByGA.put( gav( dmModel.getGroupId(), dmModel.getArtifactId(), dmModel.getVersion() ), mv );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        MavenProject project = new MavenProject( depModel.clone() );
        project.setOriginalModel( depModel );
        projects.add( project );

        project = new MavenProject( dmModel.clone() );
        project.setOriginalModel( dmModel );
        projects.add( project );

        project = new MavenProject( orig.clone() );
        project.setOriginalModel( orig );
        projects.add( project );

        final Set<MavenProject> changes = newVersioningModifier().applyVersioningChanges( projects, versionsByGA );

        assertThat( changes.size(), equalTo( 3 ) );
        for ( final MavenProject proj : changes )
        {
            final String a = proj.getArtifactId();

            if ( a.equals( "bar" ) )
            {
                assertThat( proj.getOriginalModel()
                                .getVersion(), equalTo( mv ) );
                assertThat( proj.getModel()
                                .getVersion(), equalTo( mv ) );

                final Profile op = proj.getOriginalModel()
                                       .getProfiles()
                                       .get( 0 );
                final Profile ep = proj.getModel()
                                       .getProfiles()
                                       .get( 0 );

                List<Dependency> deps = op.getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                Dependency d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = ep.getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = op.getDependencyManagement()
                         .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = ep.getDependencyManagement()
                         .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

            }
            else
            {
                assertThat( proj.getOriginalModel()
                                .getVersion(), equalTo( mv ) );
                assertThat( proj.getModel()
                                .getVersion(), equalTo( mv ) );
            }
        }
    }

    @Test
    public void updateEffectiveAndOriginalModelDependencyVersions_OnlyWhenHasVersion_InProfile()
        throws Exception
    {
        final Model orig = new Model();
        orig.setGroupId( "org.foo" );
        orig.setArtifactId( "bar" );
        orig.setVersion( "1.0" );

        final Model depModel = new Model();
        depModel.setGroupId( "org.foo" );
        depModel.setArtifactId( "bar-dep" );
        depModel.setVersion( "1.0" );

        final Dependency dep = new Dependency();
        dep.setGroupId( depModel.getGroupId() );
        dep.setArtifactId( depModel.getArtifactId() );

        final Profile p = new Profile();
        p.setId( "test" );
        orig.addProfile( p );

        p.addDependency( dep );

        final DependencyManagement mgmt = new DependencyManagement();

        final Dependency managed = new Dependency();
        managed.setGroupId( depModel.getGroupId() );
        managed.setArtifactId( depModel.getArtifactId() );
        managed.setVersion( depModel.getVersion() );

        mgmt.addDependency( managed );
        p.setDependencyManagement( mgmt );

        final String suff = "redhat-1";
        final String mv = orig.getVersion() + "." + suff;

        final Map<String, String> versionsByGA = new HashMap<String, String>();
        versionsByGA.put( gav( orig.getGroupId(), orig.getArtifactId(), orig.getVersion() ), mv );
        versionsByGA.put( gav( depModel.getGroupId(), depModel.getArtifactId(), depModel.getVersion() ), mv );

        final List<MavenProject> projects = new ArrayList<MavenProject>();

        MavenProject project = new MavenProject( depModel.clone() );
        project.setOriginalModel( depModel );
        projects.add( project );

        project = new MavenProject( orig.clone() );
        project.setOriginalModel( orig );
        projects.add( project );

        final Set<MavenProject> changes = newVersioningModifier().applyVersioningChanges( projects, versionsByGA );

        assertThat( changes.size(), equalTo( 2 ) );
        for ( final MavenProject proj : changes )
        {
            final String a = proj.getArtifactId();

            if ( a.equals( "bar" ) )
            {
                assertThat( proj.getOriginalModel()
                                .getVersion(), equalTo( mv ) );
                assertThat( proj.getModel()
                                .getVersion(), equalTo( mv ) );

                final Profile op = proj.getOriginalModel()
                                       .getProfiles()
                                       .get( 0 );
                final Profile ep = proj.getModel()
                                       .getProfiles()
                                       .get( 0 );

                List<Dependency> deps = op.getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                Dependency d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), nullValue() );

                deps = ep.getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), nullValue() );

                deps = op.getDependencyManagement()
                         .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

                deps = ep.getDependencyManagement()
                         .getDependencies();
                assertThat( deps.size(), equalTo( 1 ) );
                d = deps.get( 0 );

                assertThat( d, notNullValue() );
                assertThat( d.getVersion(), equalTo( mv ) );

            }
            else
            {
                assertThat( proj.getOriginalModel()
                                .getVersion(), equalTo( mv ) );
                assertThat( proj.getModel()
                                .getVersion(), equalTo( mv ) );
            }
        }
    }

    private VersioningModifier newVersioningModifier()
    {
        return new VersioningModifier( new DefaultModelWriter(), new ConsoleLogger() );
    }

}
