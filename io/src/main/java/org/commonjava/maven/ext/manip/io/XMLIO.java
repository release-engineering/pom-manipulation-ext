/**
 *  Copyright (C) 2016 Red Hat, Inc (jcasey@redhat.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.commonjava.maven.ext.manip.io;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;

@Component( role = XMLIO.class )
public class XMLIO
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    private final DocumentBuilder builder;
    private final Transformer transformer;

    public XMLIO()
    {
        try
        {
            builder = builderFactory.newDocumentBuilder();

            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty( OutputKeys.INDENT, "yes");
            transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
        }
        catch ( ParserConfigurationException e )
        {
            logger.error( "Unable to create new DocumentBuilder", e );
            throw new RuntimeException("Unable to create new DocumentBuilder" );
        }
        catch ( TransformerConfigurationException e )
        {
            logger.error( "Unable to create new Transformer", e );
            throw new RuntimeException("Unable to create new Transformer" );
        }
    }


    public Document parseXML ( final File xmlFile) throws ManipulationException
    {
        if ( xmlFile == null || !xmlFile.exists() )
        {
            throw new ManipulationException( "JSON File not found");
        }
        Document doc;
        try
        {
            doc = builder.parse( xmlFile);
        }
        catch ( SAXException | IOException e )
        {
            logger.error( "Unable to parse XML File {} ", e );
            throw new ManipulationException( "Unable to parse JSON File", e );
        }
        return doc;
    }


    public void writeXML (File target, Document contents) throws ManipulationException
    {
        try
        {
            // TODO: https://stackoverflow.com/questions/24551962/adding-linebreak-in-xml-file-before-root-node
            // TODO: Can't get a clean round trip due to newline differences.
            // StreamResult result = new StreamResult( new FileWriter( target));
            // transformer.transform( new DOMSource( contents ), result);
            String result = convert( contents );
            // Adjust for comment before root node and possibly insert a newline.
            result = result.replaceFirst("(?s)(<!--.*-->)<", "$1\n<");
            FileUtils.writeStringToFile(target, result);
        }
        catch ( IOException e )
        {
            logger.error( "XML transformer failure", e );
            throw new ManipulationException( "XML transformer failure", e );
        }
    }

    public String convert( Document contents ) throws ManipulationException
    {
        StringWriter outWriter = new StringWriter();
        try
        {
            StreamResult streamResult = new StreamResult( outWriter );
            transformer.transform( new DOMSource( contents ), streamResult);
        }
        catch ( TransformerException e )
        {
            logger.error( "XML transformer failure", e );
            throw new ManipulationException( "XML transformer failure", e );
        }
        return outWriter.toString();
    }
}
