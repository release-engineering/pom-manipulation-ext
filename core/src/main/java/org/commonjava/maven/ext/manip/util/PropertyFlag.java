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
package org.commonjava.maven.ext.manip.util;

/**
 * Wrapper to hold mapping of deprecated property name and current name
 */
public class PropertyFlag
{
    private final String deprecated;

    private final String current;

    /**
     * @param deprecated the original deprecated property key.
     * @param currentName the new property key.
     */
    public PropertyFlag( String deprecated, String currentName )
    {
        this.deprecated = deprecated;
        this.current = currentName;
    }

    public String getDeprecated()
    {
        return deprecated;
    }

    public String getCurrent()
    {
        return current;
    }
}
