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

package org.commonjava.maven.ext.common.json;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.jboss.da.reports.model.response.LookupReport;

@ToString
@Setter
@Getter
public class ExtendedLookupReport extends LookupReport
{
    private ProjectVersionRef projectVersionRef;

    public ExtendedLookupReport ()
    {
        super(null);
    }
}