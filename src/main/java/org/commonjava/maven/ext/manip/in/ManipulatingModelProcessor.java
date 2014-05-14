package org.commonjava.maven.ext.manip.in;

import static org.commonjava.maven.ext.manip.IdUtils.ga;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.model.locator.ModelLocator;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationManager;
import org.commonjava.maven.ext.manip.state.ManipulationSession;

/**
 * {@link ModelProcessor} implementation to override {@link DefaultModelProcessor} and inject versioning modifications.
 * This is a hook implementation used from within Maven's core. It will not be referenced directly in this project, 
 * BUT THAT DOES NOT MEAN IT'S NOT USED.
 * 
 * It requires that a valid components.xml file be generated during the build that will define this implementation as the default component for the 
 * {@link ModelProcessor} interface, so it will override the one in maven core. See the <code>plexus-component-metadata</code> plugin config in the
 * POM for more information.
 * 
 * @author jdcasey
 */
@Component( role = ModelProcessor.class )
public class ManipulatingModelProcessor
    implements ModelProcessor
{

    @Requirement
    private ModelLocator locator;

    @Requirement
    private ModelReader reader;

    @Requirement
    private ManipulationManager manipulationManager;

    // FIXME: This was a classic getInstance() singleton...injection MAY not work here.
    @Requirement
    private ManipulationSession session;

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
        if ( !session.isEnabled() )
        {
            logger.debug( "[VERSION-EXT] " + model.toString() + ": Versioning session disabled. Skipping modification." );
            return;
        }

        final Set<String> changed = session.getChangedGAs();
        try
        {
            if ( manipulationManager.applyManipulations( model, session ) )
            {
                logger.debug( "[VERSION-EXT] " + model.toString() + ": Version modified." );
                changed.add( ga( model ) );
            }
            else
            {
                logger.debug( "[VERSION-EXT] " + model.toString() + ": No version modifications. Skipping modification." );
            }
        }
        catch ( final ManipulationException e )
        {
            throw new IOException( "Manipulation failed: " + e.getMessage(), e );
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

}
