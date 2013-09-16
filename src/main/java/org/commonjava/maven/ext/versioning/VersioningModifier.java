package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.ga;
import static org.commonjava.maven.ext.versioning.IdUtils.gav;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Component( role = VersioningModifier.class )
public class VersioningModifier
{

    @Requirement
    private Logger logger;

    @Requirement
    private VersionCalculator calculator;

    public VersioningModifier()
    {
    }

    public VersioningModifier( final ModelWriter writer, final Logger logger )
    {
        this.logger = logger;
    }

    public Set<MavenProject> apply( final Collection<MavenProject> projects, final Properties userProperties )
        throws InterpolationException, VersionModifierException
    {
        final VersioningSession session = VersioningSession.getInstance();
        if ( !session.isEnabled() )
        {
            logger.info( "Versioning Extension: Nothing to do!" );
            return Collections.emptySet();
        }

        logger.info( "Versioning Extension: Applying version suffix." );
        final Map<String, String> versionsByGAV = calculator.calculateVersioningChanges( projects );
        if ( versionsByGAV.isEmpty() )
        {
            return Collections.emptySet();
        }

        return applyVersioningChanges( projects, versionsByGAV );
    }

    protected Set<MavenProject> applyVersioningChanges( final Collection<MavenProject> projects,
                                                        final Map<String, String> versionsByGAV )
        throws InterpolationException
    {
        final Set<MavenProject> changed = new HashSet<MavenProject>();
        for ( final MavenProject project : projects )
        {
            if ( applyVersioningChanges( project.getOriginalModel(), versionsByGAV ) )
            {
                final String v = versionsByGAV.get( gav( project ) );
                logger.info( project.getName() + " (" + gav( project ) + "): VERSION MODIFIED\n    New version: " + v );

                // this is a bigger model, so only do this if the originalModel was modded.
                applyVersioningChanges( project.getModel(), versionsByGAV );
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

    public boolean applyVersioningChanges( final Model model, final Map<String, String> versionsByGAV )
        throws InterpolationException
    {
        boolean changed = false;

        if ( versionsByGAV == null || versionsByGAV.isEmpty() )
        {
            return false;
        }

        // logger.info( "Looking for applicable versioning changes in: " + gav( model ) );

        String g = model.getGroupId();
        String v = model.getVersion();
        final Parent parent = model.getParent();

        // If the groupId or version is null, it means they must be taken from the parent config
        if ( g == null && parent != null )
        {
            g = parent.getGroupId();
        }
        if ( v == null && parent != null )
        {
            v = parent.getVersion();
        }

        // If the parent version is defined, it might be necessary to change it
        // If the parent version is not defined, it will be taken automatically from the project version
        if ( parent != null && parent.getVersion() != null )
        {
            final String parentGAV = gav( parent.getGroupId(), parent.getArtifactId(), parent.getVersion() );
            if ( versionsByGAV.containsKey( parentGAV ) )
            {
                final String newVersion = versionsByGAV.get( parentGAV );
                parent.setVersion( newVersion );
                changed = true;
            }
        }

        String gav = gav( g, model.getArtifactId(), v );
        if ( model.getVersion() != null )
        {
            final String newVersion = versionsByGAV.get( gav );
            if ( newVersion != null && model.getVersion() != null )
            {
                model.setVersion( newVersion );
                // logger.info( "Changed main version in " + gav( model ) );
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

        final StringSearchInterpolator interp = new StringSearchInterpolator();
        if ( model.getProperties() != null )
        {
            interp.addValueSource( new PropertiesBasedValueSource( model.getProperties() ) );
        }

        final List<String> prefixes = Arrays.asList( "pom", "project" );
        interp.addValueSource( new PrefixedObjectValueSource( prefixes, model, true ) );

        final RecursionInterceptor ri = new PrefixAwareRecursionInterceptor( prefixes, true );

        for ( final ModelBase base : bases )
        {
            final DependencyManagement dm = base.getDependencyManagement();
            if ( dm != null && dm.getDependencies() != null )
            {
                for ( final Dependency d : dm.getDependencies() )
                {
                    gav = gav( interp.interpolate( d.getGroupId(), ri ), interp.interpolate( d.getArtifactId(), ri ), interp.interpolate( d.getVersion(), ri ) );
                    final String newVersion = versionsByGAV.get( gav );
                    if ( newVersion != null )
                    {
                        d.setVersion( newVersion );
                        // logger.info( "Changed managed: " + d + " in " + base );
                        changed = true;
                    }
                }
            }

            if ( base.getDependencies() != null )
            {
                for ( final Dependency d : base.getDependencies() )
                {
                    gav = gav( interp.interpolate( d.getGroupId(), ri ), interp.interpolate( d.getArtifactId(), ri ), interp.interpolate( d.getVersion(), ri ) );
                    final String newVersion = versionsByGAV.get( gav );
                    if ( newVersion != null && d.getVersion() != null )
                    {
                        d.setVersion( newVersion );
                        // logger.info( "Changed: " + d + " in " + base );
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
        throws IOException, XmlPullParserException, InterpolationException
    {
        final VersioningSession session = VersioningSession.getInstance();
        final Set<String> changed = session.getChangedGAVs();

        final Map<String, String> changes = session.getVersioningChanges();

        final File marker = session.getMarkerFile();
        PrintWriter pw = null;
        try
        {
            marker.getParentFile().mkdirs();

            pw = new PrintWriter( new FileWriter( marker ) );

            for ( final MavenProject project : projects )
            {
                final String ga = ga( project );
                if ( changed.contains( ga ) )
                {
                    File pom = project.getFile();
                    if ( pom.getName().equals( "interpolated-pom.xml" ) )
                    {
                        final File dir = pom.getParentFile();
                        pom = dir == null ? new File( "pom.xml" ) : new File( dir, "pom.xml" );
                    }

                    logger.info( "Rewriting: " + project.getId() + "\n       to POM: " + pom );

                    final MavenXpp3Writer writer = new MavenXpp3Writer();

                    Reader pomReader = null;
                    Model model;
                    try
                    {
                        pomReader = ReaderFactory.newXmlReader( pom );
                        model = new MavenXpp3Reader().read( pomReader );
                    }
                    finally
                    {
                        IOUtil.close( pomReader );
                    }

                    applyVersioningChanges( model, changes );

                    Writer pomWriter = null;
                    try
                    {
                        pomWriter = WriterFactory.newXmlWriter( pom );
                        writer.write( pomWriter, model );
                    }
                    finally
                    {
                        IOUtil.close( pomWriter );
                    }

                    pw.println( project.getId() );
                }
            }
        }
        finally
        {
            IOUtil.close( pw );
        }
    }

}
