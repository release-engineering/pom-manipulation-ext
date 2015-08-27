/**
 *  Copyright (C) 2015 Red Hat, Inc (jcasey@redhat.com)
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

package org.commonjava.maven.ext.manip.invoker;

import java.util.Map;

/**
 * @author vdedik@redhat.com
 */
public class Execution
{
    private String mvnCommand;

    private String location;

    private Boolean success = true;

    private Map<String, String> javaParams;

    public Map<String, String> getJavaParams()
    {
        return javaParams;
    }

    public void setJavaParams( Map<String, String> javaParams )
    {
        this.javaParams = javaParams;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    public String getMvnCommand()
    {
        return mvnCommand;
    }

    public void setMvnCommand( String mvnCommand )
    {
        this.mvnCommand = mvnCommand;
    }

    public Boolean isSuccess()
    {
        return success;
    }

    public void setSuccess( Boolean success )
    {
        this.success = success;
    }
}
