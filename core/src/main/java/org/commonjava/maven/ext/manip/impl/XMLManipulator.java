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

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.commonjava.maven.ext.manip.ManipulationSession;
import org.commonjava.maven.ext.manip.io.XMLIO;
import org.commonjava.maven.ext.manip.model.Project;
import org.commonjava.maven.ext.manip.state.XMLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * {@link Manipulator} implementation that can modify XML files. Configuration
 * is stored in a {@link XMLState} instance, which is in turn stored in the {@link ManipulationSession}.
 */
@Component( role = Manipulator.class, hint = "xml-manipulator" )
public class XMLManipulator
    implements Manipulator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private XPath xPath = XPathFactory.newInstance().newXPath();

    @Requirement
    private XMLIO xmlIO;

    @Override
    public void scan( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
    }

    /**
     * Initialize the {@link XMLState} state holder in the {@link ManipulationSession}. This state holder detects
     * configuration from the Maven user properties (-D properties from the CLI) and makes it available for
     * later invocations of {@link XMLManipulator#scan(List, ManipulationSession)} and the apply* methods.
     */
    @Override
    public void init( final ManipulationSession session )
                    throws ManipulationException
    {
        final Properties userProps = session.getUserProperties();
        session.setState( new XMLState( userProps ) );
    }

    /**
     * Apply the xml changes to the specified file(s).
     */
    @Override
    public Set<Project> applyChanges( final List<Project> projects, final ManipulationSession session )
        throws ManipulationException
    {
        final XMLState state = session.getState( XMLState.class );
        if ( !session.isEnabled() || !state.isEnabled() )
        {
            logger.debug( getClass().getSimpleName() + ": Nothing to do!" );
            return Collections.emptySet();
        }

        final Set<Project> changed = new HashSet<>();
        final List<XMLState.XMLOperation> scripts = state.getXMLOperations();

        for ( final Project project : projects )
        {
            if ( project.isExecutionRoot() )
            {
                for ( XMLState.XMLOperation operation : scripts )
                {
                    File target = new File( project.getPom().getParentFile(), operation.getFile() );

                    logger.info( "Attempting to start XML update to file {} with xpath {} and replacement {}",
                                 target, operation.getXPath(), operation.getUpdate() );

                    internalApplyChanges (target, operation);

                    changed.add( project );
                }

                break;
            }
        }
        return changed;
    }

    void internalApplyChanges( File target, XMLState.XMLOperation operation ) throws ManipulationException
    {
        Document doc = xmlIO.parseXML( target );

        try
        {
            NodeList nodeList = (NodeList) xPath.evaluate( operation.getXPath(), doc, XPathConstants.NODESET );

            if ( nodeList.getLength() == 0 )
            {
                throw new ManipulationException( "Did not locate XML using XPath " + operation.getXPath() );
            }

            for ( int i = 0; i < nodeList.getLength(); i++ )
            {
                Node node = nodeList.item( i );

                if ( isEmpty( operation.getUpdate() ) )
                {
                    // Delete
                    node.getParentNode().removeChild( node );
                }
                else
                {
                    // Update
                    node.setTextContent( operation.getUpdate() );
                }
            }

            xmlIO.writeXML( target, doc );
        }
        catch ( XPathExpressionException e )
        {
            logger.error( "Caught XML exception processing file {}, document context {} ", target, doc, e );
            throw new ManipulationException( "Caught XML exception processing file", e );
        }
    }

    @Override
    public int getExecutionIndex()
    {
        return 98;
    }
}
