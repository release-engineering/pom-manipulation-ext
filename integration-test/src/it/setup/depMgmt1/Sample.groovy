/**
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
package org.commonjava.maven.ext.depMgmt1

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.BaseScript
import groovy.util.logging.Slf4j

@BaseScript org.commonjava.maven.ext.core.groovy.BaseScript pme

@Slf4j
class Processor {
    File basedir
    Properties props

    private void parseDeps(Map n) {
        def iterator = n.iterator()
        while (iterator.hasNext()) {
            def entry = iterator.next()

            if (entry != null && entry.getKey() == "resolved") {
                iterator.remove()
            }

            if (entry != null && entry.getValue() instanceof Map) {
                parseDeps(entry.getValue())
            }
        }
    }

    def execute() {
        log.info("Running ShrinkwrapProcessor..." )
        log.info("groovyScripts " + props.get("groovyScripts"))

        def shrinkwrap = new File (basedir.toString() + File.separator + "shrink.json")

        log.info("shrinkwrap json is " + shrinkwrap)

        if (shrinkwrap.exists()) {
            log.info ("Found file {}", shrinkwrap)

            // Annoyingly the JsonSlurper reads them and does NOT maintain the order. Therefore
            // the diff is not overly helpful
            LinkedHashMap json = new JsonSlurper().parseText(shrinkwrap.text)

            parseDeps(json)

            shrinkwrap.write(JsonOutput.prettyPrint(JsonOutput.toJson(json)))
        }
    }
}
// These are both debug AND test statements - do NOT remove. If the injection (in GroovyManipulator)
// fails these prints will cause the test to fail.
println "#### BASESCRIPT:"
println pme.getBaseDir()
println pme.getBaseDir().getClass().getName()
println pme.getGAV()
println pme.getGAV().getClass().getName()
println pme.getProjects()
println pme.getProject().getClass().getName()
println pme.getProjects()
println pme.getProject().getClass().getName()
println "#### BASESCRIPT END"

// End...

def Processor sp = new Processor(basedir:pme.getBaseDir(), props:pme.getUserProperties())
sp.execute()
