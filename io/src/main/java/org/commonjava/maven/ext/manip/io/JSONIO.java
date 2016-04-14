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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import org.codehaus.plexus.component.annotations.Component;
import org.commonjava.maven.ext.manip.ManipulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component( role = JSONIO.class )
public class JSONIO
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private ObjectMapper mapper = new ObjectMapper( );

    private Configuration conf = Configuration.builder().options( Option.ALWAYS_RETURN_LIST ).build();

    private PrettyPrinter dpp = new MyPrettyPrinter();

    public DocumentContext parseJSON (final File jsonFile) throws ManipulationException
    {
        if ( jsonFile == null || !jsonFile.exists() )
        {
            throw new ManipulationException( "JSON File not found");
        }
        DocumentContext doc;
        try
        {
            doc = JsonPath.using( conf ).parse( jsonFile);
        }
        catch ( IOException e )
        {
            logger.error( "Unable to parse JSON File {} ", e );
            throw new ManipulationException( "Unable to parse JSON File", e );
        }
        return doc;
    }


    public void writeJSON (File target, String contents) throws ManipulationException
    {
        try
        {
            String pretty = mapper.writer(dpp).writeValueAsString( mapper.readValue( contents, Object.class ) );

            try ( FileWriter p = new FileWriter( target ) )
            {
                p.write( pretty );
                p.append( '\n' );
            }
        }
        catch ( IOException e )
        {
            logger.error( "Unable to write JSON string:  ", e );
            throw new ManipulationException( "Unable to write JSON string", e );
        }
    }

    /**
     * Override the default Jackson pretty printer so the object field separators look like
     *       "version": "0.1.0",
     * not
     *       "version" : "0.1.0",
     */
    private class MyPrettyPrinter extends DefaultPrettyPrinter
    {
        public MyPrettyPrinter() {
            super(DEFAULT_ROOT_VALUE_SEPARATOR);
        }

        public MyPrettyPrinter(MyPrettyPrinter base) {
            super(base, base._rootSeparator);
        }

        @Override
        public DefaultPrettyPrinter createInstance() {
            return new MyPrettyPrinter(this);
        }

        @Override
        public void writeObjectFieldValueSeparator( JsonGenerator jg )
                        throws IOException, JsonGenerationException
        {
            if ( _spacesInObjectEntries )
            {
                jg.writeRaw( ": " );
            }
            else
            {
                jg.writeRaw( ':' );
            }
        }
    }
}
