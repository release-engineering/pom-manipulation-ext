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

package org.commonjava.maven.ext.manip.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.jboss.byteman.rule.helper.Helper;

import java.util.List;

public class HttpRequestWithBodyBytemanHelper
                extends Helper
{

    protected HttpRequestWithBodyBytemanHelper( org.jboss.byteman.rule.Rule rule )
    {
        super( rule );
    }

    public List<ProjectVersionRef> asOtherClass( Object object)
    {
        if (object instanceof List)
        {
            return (List<ProjectVersionRef>) object;
        }
        return null;
    }
}
