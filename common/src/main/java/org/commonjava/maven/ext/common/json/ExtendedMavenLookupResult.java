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
package org.commonjava.maven.ext.common.json;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.goots.hiderdoclet.doclet.JavadocExclude;
import org.jboss.da.lookup.model.MavenLookupResult;
import org.jboss.da.model.rest.GAV;
import org.jboss.da.reports.model.response.LookupReport;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true)
@JavadocExclude
public class ExtendedMavenLookupResult
                extends MavenLookupResult
{
    private ProjectVersionRef projectVersionRef;

    public ExtendedMavenLookupResult(@NonNull GAV gav, String bestMatchVersion )
    {
        super( gav, bestMatchVersion );
    }

    @Override
    public String toString ()
    {
        return "PVR : " + projectVersionRef + " ; BestMatch : " + getBestMatchVersion();
    }
}
