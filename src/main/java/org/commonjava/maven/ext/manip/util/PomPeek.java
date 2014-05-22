/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.maven.ext.manip.util;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.join;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class PomPeek
{
    private static final String G = "g";

    private static final String A = "a";

    private static final String V = "v";

    private static final String PG = "pg";

    private static final String PA = "pa";

    private static final String PV = "pv";

    private static final String PKG = "pkg";

    private static final String PRP = "prp";

    private static final String[] COORD_KEYS = { G, A, V, PG, PA, PV, PKG, PRP };

    private static final String MODULE_ELEM = "module";

    private static final String MODULES_ELEM = "modules";

    private static final Map<String, String> CAPTURED_PATHS = new HashMap<String, String>()
    {
        private static final long serialVersionUID = 1L;

        {
            put( "project:groupId", G );
            put( "project:artifactId", A );
            put( "project:version", V );
            put( "project:packaging", PKG );
            put( "project:parent:groupId", PG );
            put( "project:parent:artifactId", PA );
            put( "project:parent:version", PV );
            put( "project:parent:relativePath", PRP );
        }
    };

    @Requirement
    private Logger logger;

    private ProjectVersionRef key;

    private final Map<String, String> elementValues = new HashMap<String, String>();

    private final Set<String> modules = new HashSet<String>();

    private final File pom;

    private ProjectVersionRef parentKey;

    private boolean modulesDone = false;

    /**
     * Denotes if this represents the top level peeked POM.
     */
    private boolean topPOM = false;

    public PomPeek( final File pom )
    {
        this.pom = pom;
        parseCoordElements( pom );

        if ( !createCoordinateInfo() )
        {
            logger.warn( "Could not peek at POM coordinate for: " + pom
                + "This POM will NOT be available as an ancestor to other models during effective-model building." );
        }
    }

    public String getParentRelativePath()
    {
        return elementValues.get( PRP );
    }

    public Set<String> getModules()
    {
        return modules;
    }

    public File getPom()
    {
        return pom;
    }

    public ProjectVersionRef getKey()
    {
        return key;
    }

    public ProjectVersionRef getParentKey()
    {
        return parentKey;
    }

    private void parseCoordElements( final File pom )
    {
        Reader reader = null;
        XMLStreamReader xml = null;
        try
        {
            reader = new FileReader( pom );
            xml = XMLInputFactory.newFactory()
                                 .createXMLStreamReader( reader );

            final Stack<String> path = new Stack<String>();
            while ( xml.hasNext() )
            {
                final int evt = xml.next();
                switch ( evt )
                {
                    case START_ELEMENT:
                    {
                        final String elem = xml.getLocalName();
                        path.push( elem );
                        if ( captureValue( elem, path, xml ) )
                        {
                            // seems like xml.getElementText() traverses the END_ELEMENT event...
                            path.pop();
                        }
                        break;
                    }
                    case END_ELEMENT:
                    {
                        final String elem = xml.getLocalName();
                        if ( MODULES_ELEM.equals( elem ) )
                        {
                            modulesDone = true;
                        }

                        path.pop();
                        break;
                    }
                    default:
                    {
                    }
                }

                if ( foundAll() )
                {
                    return;
                }
            }
        }
        catch ( final IOException e )
        {
            logger.warn( "Failed to peek at POM coordinate for: " + pom + " Reason: " + e.getMessage()
                + "\nThis POM will NOT be available as an ancestor to other models during effective-model building.", e );
        }
        catch ( final XMLStreamException e )
        {
            logger.warn( "Failed to peek at POM coordinate for: " + pom + " Reason: " + e.getMessage()
                + "\nThis POM will NOT be available as an ancestor to other models during effective-model building.", e );
        }
        catch ( final FactoryConfigurationError e )
        {
            logger.warn( "Failed to peek at POM coordinate for: " + pom + " Reason: " + e.getMessage()
                + "\nThis POM will NOT be available as an ancestor to other models during effective-model building.", e );
        }
        finally
        {
            if ( xml != null )
            {
                try
                {
                    xml.close();
                }
                catch ( final XMLStreamException e )
                {
                    logger.warn( "Failed to close XMLStreamReader: " + e.getMessage(), e );
                }
                finally
                {
                }
            }

            closeQuietly( reader );
        }
    }

    private boolean foundAll()
    {
        for ( final String key : COORD_KEYS )
        {
            if ( !elementValues.containsKey( key ) )
            {
                return false;
            }
        }

        if ( "pom".equals( elementValues.get( PKG ) ) && !modulesDone )
        {
            return false;
        }

        return true;
    }

    private boolean captureValue( final String elem, final Stack<String> path, final XMLStreamReader xml )
        throws XMLStreamException
    {
        final String pathStr = join( path, ":" );
        final String key = CAPTURED_PATHS.get( pathStr );
        if ( key != null )
        {
            elementValues.put( key, xml.getElementText()
                                       .trim() );

            return true;
        }
        else if ( MODULE_ELEM.equals( elem ) )
        {
            modules.add( xml.getElementText()
                            .trim() );
        }

        return false;
    }

    private boolean createCoordinateInfo()
    {
        String v = elementValues.get( V );
        final String pv = elementValues.get( PV );
        if ( isEmpty( v ) )
        {
            v = pv;
        }

        String g = elementValues.get( G );
        final String pg = elementValues.get( PG );
        if ( isEmpty( g ) )
        {
            g = pg;
        }

        final String a = elementValues.get( A );
        final String pa = elementValues.get( PA );

        boolean valid = false;
        if ( isValidArtifactId( a ) && isValidGroupId( g ) && isValidVersion( v ) )
        {
            key = new ProjectVersionRef( g, a, v );
            valid = true;
        }

        if ( isValidArtifactId( pa ) && isValidGroupId( pg ) && isValidVersion( pv ) )
        {
            parentKey = new ProjectVersionRef( pg, pa, pv );
        }

        return valid;
    }

    private boolean isValidVersion( final String version )
    {
        if ( isEmpty( version ) )
        {
            return false;
        }

        if ( "version".equals( version ) )
        {
            return false;
        }

        if ( "parentVersion".equals( version ) )
        {
            return false;
        }

        return true;
    }

    private boolean isValidGroupId( final String groupId )
    {
        if ( isEmpty( groupId ) )
        {
            return false;
        }

        if ( groupId.contains( "${" ) )
        {
            return false;
        }

        if ( "parentGroupId".equals( groupId ) )
        {
            return false;
        }

        if ( "groupId".equals( groupId ) )
        {
            return false;
        }

        return true;
    }

    private boolean isValidArtifactId( final String artifactId )
    {
        if ( isEmpty( artifactId ) )
        {
            return false;
        }

        if ( artifactId.contains( "${" ) )
        {
            return false;
        }

        if ( "parentArtifactId".equals( artifactId ) )
        {
            return false;
        }

        if ( "artifactId".equals( artifactId ) )
        {
            return false;
        }

        return true;
    }

    public void setTopPOM( final boolean b )
    {
        topPOM = b;
    }

    public boolean isTopPOM()
    {
        return topPOM;
    }
}
