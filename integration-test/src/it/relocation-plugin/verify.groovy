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

def plugin1 = pom.build.plugins.plugin.find { it.groupId.text() == "org.wildfly.plugins" && it.artifactId.text() == "wildfly-maven-plugin" }
assert plugin1.size() != 0
assert plugin1.executions.execution.configuration.groupId.text() == "org.wildfly"
assert plugin1.executions.execution.configuration.artifactId.text() == "wildfly-dist"
assert plugin1.executions.execution.configuration.version.text() == "18.0.1.Final"

def plugin2 = pom.build.plugins.plugin.find { it.groupId.text() == "org.apache.maven.plugins" && it.artifactId.text() == "maven-dependency-plugin" }
assert plugin2.size() != 0

def artifactItem1 = plugin2.executions.execution.configuration.artifactItems.artifactItem.find { it.groupId.text() == "junit" && it.artifactId.text() == "junit" }
assert artifactItem1.size() != 0
assert artifactItem1.version.text() == "4.1"

def artifactItem2 = plugin2.executions.execution.configuration.artifactItems.artifactItem.find { it.groupId.text() == "org.slf4j" && it.artifactId.text() == "slf4j-api" }
assert artifactItem2.size() != 0
assert artifactItem2.version.text() == "1.7.30"
