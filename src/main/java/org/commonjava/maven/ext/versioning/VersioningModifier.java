package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.ga;
import static org.commonjava.maven.ext.versioning.IdUtils.gav;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.ModelBase;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.ModelWriter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component( role = VersioningModifier.class )
public class VersioningModifier
{

    private static final Map<String, Object> OPTIONS;

    static
    {
        final Map<String, Object> opts = new HashMap<String, Object>();

        OPTIONS = opts;
    }

    @Requirement
    private Logger logger;

    @Requirement
    private ModelWriter writer;

    public VersioningModifier()
    {
    }

    public VersioningModifier( final ModelWriter writer, final Logger logger )
    {
        this.logger = logger;
    }

    public Set<MavenProject> apply( final Collection<MavenProject> projects, final Properties userProperties )
    {
        final VersionCalculator calculator = new VersionCalculator( userProperties );
        if ( !calculator.isEnabled() )
        {
            logger.info( "Versioning Extension: Nothing to do!" );
            return Collections.emptySet();
        }

        logger.info( "Versioning Extension: Applying version suffix: " + calculator.getSuffix() );
        final Map<String, String> versionsByGA = calculator.calculateVersioningChanges( projects );
        if ( versionsByGA.isEmpty() )
        {
            return Collections.emptySet();
        }

        return applyVersioningChanges( projects, versionsByGA );
    }

    protected Set<MavenProject> applyVersioningChanges( final Collection<MavenProject> projects,
                                                        final Map<String, String> versionsByGA )
    {
        final Set<MavenProject> changed = new HashSet<MavenProject>();
        for ( final MavenProject project : projects )
        {
            if ( applyVersioningChanges( project.getOriginalModel(), versionsByGA ) )
            {
                final String v = versionsByGA.get( ga( project.getGroupId(), project.getArtifactId() ) );
                logger.info( project.getName() + " (" + gav( project ) + "): VERSION MODIFIED\n    New version: " + v );

                // this is a bigger model, so only do this if the originalModel was modded.
                applyVersioningChanges( project.getModel(), versionsByGA );
                changed.add( project );

                if ( v != null )
                {
                    // belt and suspenders...be double sure this gets set everywhere.
                    project.setVersion( v );
                }
            }
        }

        return changed;
    }

    public boolean applyVersioningChanges( final Model model, final Map<String, String> versionsByGA )
    {
        boolean changed = false;

        if ( versionsByGA == null || versionsByGA.isEmpty() )
        {
            return changed;
        }

        logger.info( "Looking for applicable versioning changes in: " + gav( model ) );

        String g = model.getGroupId();
        final Parent originalParent = model.getParent();
        if ( originalParent != null )
        {
            if ( g == null )
            {
                g = originalParent.getGroupId();
            }

            final String parentGA = ga( originalParent.getGroupId(), originalParent.getArtifactId() );
            final String v = versionsByGA.get( parentGA );
            if ( versionsByGA.containsKey( parentGA ) )
            {
                originalParent.setVersion( v );
                changed = true;
            }
        }

        String ga = ga( g, model.getArtifactId() );
        if ( model.getVersion() != null )
        {
            final String v = versionsByGA.get( ga );
            if ( v != null && model.getVersion() != null )
            {
                model.setVersion( v );
                logger.info( "Changed main version in " + gav( model ) );
                changed = true;
            }
        }

        final Set<ModelBase> bases = new HashSet<ModelBase>();
        bases.add( model );

        final List<Profile> profiles = model.getProfiles();
        if ( profiles != null )
        {
            bases.addAll( profiles );
        }

        for ( final ModelBase base : bases )
        {
            final DependencyManagement dm = base.getDependencyManagement();
            if ( dm != null && dm.getDependencies() != null )
            {
                for ( final Dependency d : dm.getDependencies() )
                {
                    ga = ga( d.getGroupId(), d.getArtifactId() );
                    final String v = versionsByGA.get( ga );
                    if ( v != null )
                    {
                        d.setVersion( v );
                        logger.info( "Changed managed: " + d + " in " + base );
                        changed = true;
                    }
                }
            }

            if ( base.getDependencies() != null )
            {
                for ( final Dependency d : base.getDependencies() )
                {
                    ga = ga( d.getGroupId(), d.getArtifactId() );
                    final String v = versionsByGA.get( ga );
                    if ( v != null && d.getVersion() != null )
                    {
                        d.setVersion( v );
                        logger.info( "Changed: " + d + " in " + base );
                        changed = true;
                    }
                }
            }
        }

        if ( changed )
        {
            logger.info( "Applied versioning changes to: " + gav( model ) );
        }

        return changed;
    }

    public void rewriteChangedPOMs( final List<MavenProject> projects )
        throws IOException
    {
        final VersioningSession session = VersioningSession.getInstance();
        final Set<String> changed = session.getChangedGAVs();
        for ( final MavenProject project : projects )
        {
            final String ga = ga( project );
            if ( changed.contains( ga ) )
            {
                File pom = project.getFile();
                if ( pom.getName()
                        .equals( "interpolated-pom.xml" ) )
                {
                    final File dir = pom.getParentFile();
                    pom = dir == null ? new File( "pom.xml" ) : new File( dir, "pom.xml" );
                }

                logger.info( "Rewriting: " + project.getId() + "\n       to POM: " + pom );
                writer.write( pom, OPTIONS, project.getOriginalModel() );
            }
        }
    }

}
