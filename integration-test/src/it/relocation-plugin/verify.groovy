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
def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def dependency = pom.dependencies.dependency.find { it.groupId.text() == "junit" }
assert dependency != null
assert dependency.artifactId.text() == "junit"
assert dependency.version.text() == "4.1"

dependency = pom.dependencies.dependency.find { it.groupId.text() == "org.slf4j" }
assert dependency != null
assert dependency.artifactId.text() == "slf4j-api"
assert dependency.version.text() == "1.7.30"

def plugin = pom.build.plugins.plugin.find { it.groupId.text() == "org.wildfly.plugins" && it.artifactId.text() == "wildfly-maven-plugin" }
assert plugin != null
assert plugin.executions.execution.configuration.groupId.text() == "org.wildfly"
assert plugin.executions.execution.configuration.artifactId.text() == "wildfly-dist"
assert plugin.executions.execution.configuration.version.text() == "18.0.1.Final"

plugin = pom.build.plugins.plugin.find { it.groupId.text() == "org.apache.maven.plugins" && it.artifactId.text() == "maven-dependency-plugin" }
assert plugin != null
def artifactItem = plugin.executions.execution.configuration.artifactItems.find { it.groupId.text() == "junit" && t.artifactId.text() == "junit" }
assert artifactItem != null
artifactItem = plugin.executions.execution.configuration.artifactItems.find { it.groupId.text() == "org.sfl4j" && t.artifactId.text() == "sfl4j-api" }
assert artifactItem != null
