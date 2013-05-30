package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.ga;
import static org.commonjava.maven.ext.versioning.IdUtils.gav;

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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

@Component( role = VersioningModifier.class, hint = "default" )
public class VersioningModifier
{

    @Requirement
    private Logger logger;

    @Requirement
    private VersionCalculator calculator;

    public VersioningModifier()
    {
    }

    public VersioningModifier( final VersionCalculator calculator, final Logger logger )
    {
        this.calculator = calculator;
        this.logger = logger;
    }

    public Set<MavenProject> apply( final Collection<MavenProject> projects, final Properties userProperties )
    {
        if ( !calculator.init( userProperties ) )
        {
            logger.info( "Versioning Extension: Nothing to do!" );
            return Collections.emptySet();
        }
        else
        {
            logger.info( "Versioning Extension: Applying version suffix: " + calculator.getSuffix() );
        }

        final Map<String, String> versionsByGA = calculateVersioningChanges( projects );
        return applyVersioningChanges( projects, versionsByGA );
    }

    protected Set<MavenProject> applyVersioningChanges( final Collection<MavenProject> projects,
                                                        final Map<String, String> versionsByGA )
    {
        final Set<MavenProject> changed = new HashSet<MavenProject>();
        for ( final MavenProject project : projects )
        {
            if ( modifyModel( project.getOriginalModel(), versionsByGA ) )
            {
                // this is a bigger model, so only do this if the originalModel was modded.
                modifyModel( project.getModel(), versionsByGA );
                changed.add( project );

                final String v = versionsByGA.get( ga( project.getGroupId(), project.getArtifactId() ) );
                if ( v != null )
                {
                    // belt and suspenders...be double sure this gets set everywhere.
                    project.setVersion( v );
                }
            }
        }

        return changed;
    }

    protected Map<String, String> calculateVersioningChanges( final Collection<MavenProject> projects )
    {
        final Map<String, String> versionsByGA = new HashMap<String, String>();

        for ( final MavenProject project : projects )
        {
            final String originalVersion = project.getVersion();
            final String modifiedVersion = calculator.calculate( originalVersion );

            if ( !modifiedVersion.equals( originalVersion ) )
            {
                versionsByGA.put( ga( project.getGroupId(), project.getArtifactId() ), modifiedVersion );
                logger.info( project.getName() + " (" + gav( project ) + "): VERSION MODIFIED\n    New version: "
                    + modifiedVersion );
            }
        }

        return versionsByGA;
    }

    protected boolean modifyModel( final Model model, final Map<String, String> versionsByGA )
    {
        boolean changed = false;

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
            model.setVersion( v );
            changed = true;
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
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

}
