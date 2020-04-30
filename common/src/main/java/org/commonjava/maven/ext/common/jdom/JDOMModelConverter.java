/*
 * Copyright (C) 2012 Red Hat, Inc.
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

package org.commonjava.maven.ext.common.jdom;

import org.apache.maven.model.Model;
import org.jdom.Document;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Named
@Singleton
public class JDOMModelConverter extends MavenJDOMWriter
{
    public JDOMModelConverter( )
    {
    }

    public void convertModelToJDOM ( final Model model, Document document ) throws IOException
    {
        update( model, new IndentationCounter( 0 ), document.getRootElement() );
    }
}
