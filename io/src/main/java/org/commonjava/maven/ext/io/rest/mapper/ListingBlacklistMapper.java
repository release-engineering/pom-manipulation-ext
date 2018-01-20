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
package org.commonjava.maven.ext.io.rest.mapper;

import com.mashape.unirest.http.ObjectMapper;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.io.rest.Translator.RestProtocol;
import org.commonjava.maven.ext.io.rest.exception.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rnc on 06/06/17.
 */
public class ListingBlacklistMapper
                implements ObjectMapper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

    private String errorString;

    private RestProtocol protocol;

    public ListingBlacklistMapper( RestProtocol protocol )
    {
        this.protocol = protocol;
    }

    @Override
    public Object readValue( String s )
    {
        List<ProjectVersionRef> result = new ArrayList<>();

        // Workaround for https://github.com/Mashape/unirest-java/issues/122
        // Rather than throwing an exception we return an empty body which allows
        // DefaultTranslator to examine the status codes.

        if (s.length() == 0)
        {
            errorString = "No content to read.";
            return result;
        }
        else if (s.startsWith( "<" ))
        {
            // Read an HTML string.
            String stripped = s.replaceFirst( ".*</h1>\n", "").replaceFirst( "\n</body></html>", "" );
            logger.debug( "Read HTML string '{}' rather than a JSON stream; stripping message to {}", s, stripped );

            errorString = stripped;
            return result;
        }
        else if (s.startsWith( "{\\\"message\\\":" ) || s.startsWith( "{\"message\":" ))
        {
            String endStripped = s.replace( "\\\"}", "" ).replace( "\"}", "" );
            errorString = endStripped.substring( endStripped.lastIndexOf( "\"" ) + 1 );

            logger.debug( "Read message string {}, processed to {} ", s, errorString );

            return result;
        }

        List<Map<String, Object>> responseBody;
        try
        {
            responseBody = objectMapper.readValue( s, List.class );
        }
        catch ( IOException e )
        {
            logger.error( "Failed to decode map when reading string {}", s );
            throw new RestException( "Failed to read list-of-maps response from version server: " + e.getMessage(), e );
        }

        for ( Map<String, Object> gav: responseBody )
        {
            String groupId = (String) gav.get( "groupId" );
            String artifactId = (String) gav.get( "artifactId" );
            String version = (String) gav.get( "version" );

            ProjectVersionRef project = new SimpleProjectVersionRef( groupId, artifactId, version );
            result.add ( project );
        }

        return result;
    }

    @Override
    public String writeValue( Object value )
    {
        throw new RestException( "Fatal: Should not be overriding writeObject for ListingBlacklistMapper" );
    }

    public String getErrorString()
    {
        return errorString;
    }
}
