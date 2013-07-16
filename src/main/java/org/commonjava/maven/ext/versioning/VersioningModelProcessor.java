package org.commonjava.maven.ext.versioning;

import static org.commonjava.maven.ext.versioning.IdUtils.ga;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.logging.Logger;

@Component( role = ModelProcessor.class )
public class VersioningModelProcessor
    implements ModelProcessor
{

    @Requirement
    private ModelLocator locator;

    @Requirement
    private ModelReader reader;

    @Requirement
    private VersioningModifier modder;

    @Requirement
    private Logger logger;

    @Override
    public File locatePom( final File projectDirectory )
    {
        return locator.locatePom( projectDirectory );
    }

    @Override
    public Model read( final File input, final Map<String, ?> options )
        throws IOException, ModelParseException
    {
        final Model model = reader.read( input, options );

        applyVersioning( model );

        return model;
    }

    private void applyVersioning( final Model model )
        throws IOException
    {
        final VersioningSession session = getSession();
        if ( !session.isEnabled() )
        {
            return;
        }

        final Set<String> changed = session.getChangedGAVs();
        try
        {
            if ( modder.applyVersioningChanges( model, session.getVersioningChanges() ) )
            {
                changed.add( ga( model ) );
            }
        }
        catch ( final InterpolationException e )
        {
            throw new IOException( "Interpolation failed while applying versioning changes: " + e.getMessage(), e );
        }
    }

    @Override
    public Model read( final Reader input, final Map<String, ?> options )
        throws IOException, ModelParseException
    {
        final Model model = reader.read( input, options );

        applyVersioning( model );

        return model;
    }

    @Override
    public Model read( final InputStream input, final Map<String, ?> options )
        throws IOException, ModelParseException
    {
        final Model model = reader.read( input, options );

        applyVersioning( model );

        return model;
    }

    private VersioningSession getSession()
    {
        return VersioningSession.getInstance();
    }

}
