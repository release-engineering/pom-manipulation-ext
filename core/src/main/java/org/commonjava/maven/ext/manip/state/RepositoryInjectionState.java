package org.commonjava.maven.ext.manip.state;

import java.util.Properties;

import org.commonjava.maven.atlas.ident.ref.InvalidRefException;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.ext.manip.impl.RepositoryInjectionManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures configuration relating to injection repositories from a remote POM.
 * Used by {@link RepositoryInjectionManipulator}.
 */
public class RepositoryInjectionState
    implements State
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    /**
     * Suffix to enable this modder
     */
    public static final String REPOSITORY_INJECTION_PROPERTY = "repositoryInjection";

    private final ProjectVersionRef repoMgmt;

    public RepositoryInjectionState( final Properties userProps )
    {
        final String gav = userProps.getProperty( REPOSITORY_INJECTION_PROPERTY );
        ProjectVersionRef ref = null;
        if ( gav != null )
        {
            try
            {
                ref = ProjectVersionRef.parse( gav );
            }
            catch ( final InvalidRefException e )
            {
                logger.warn( "Skipping repository injection! Got invalid repositoryInjection GAV: {}", gav );
            }
        }

        repoMgmt = ref;
    }

    /**
     * Enabled ONLY if repositoryInjection is provided in the user properties / CLI -D options.
     *
     * @see #REPOSITORY_INJECTION_PROPERTY
     * @see org.commonjava.maven.ext.manip.state.State#isEnabled()
     */
    @Override
    public boolean isEnabled()
    {
        return repoMgmt != null;
    }


    public ProjectVersionRef getRemoteRepositoryInjectionMgmt()
    {
        return repoMgmt;
    }
}

