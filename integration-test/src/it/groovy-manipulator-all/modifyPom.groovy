/*
 * Copyright (C) 2012 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import groovy.xml.XmlSlurper
import groovy.xml.XmlUtil
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import org.commonjava.maven.ext.core.groovy.BaseScript
import org.commonjava.maven.ext.core.groovy.InvocationStage
import org.commonjava.maven.ext.core.groovy.PMEBaseScript
import org.commonjava.maven.ext.core.groovy.InvocationPoint

@InvocationPoint( invocationPoint = InvocationStage.ALL )
@PMEBaseScript BaseScript pme

println "basedir=${pme.getBaseDir()}"
println "gav=${pme.getGAV()}"
println "projects=${pme.getProjects()}"
println "invocationStage=${pme.getInvocationStage()}"

final def basedir = pme.getBaseDir().toPath()
def filename = 'interpolated-pom.xml'
def pomFile = basedir.resolve( filename )

if ( !Files.exists( pomFile ) )
{
    filename = 'pom.xml'
    pomFile = basedir.resolve( filename )
}

println 'Slurping POM: ' + pomFile.toAbsolutePath()
final def pom = new XmlSlurper( false, false ).parse( pomFile )

println 'Original count: ' + pom.properties.manipulatorCount
pom.properties.manipulatorCount = ( pom.properties.manipulatorCount.toInteger() + 1 ).toString()
println 'New count: ' + pom.properties.manipulatorCount

final def xml = XmlUtil.serialize( pom )
println "Write ${pomFile}"
Files.write( pomFile, xml.getBytes( StandardCharsets.UTF_8.name() ), StandardOpenOption.TRUNCATE_EXISTING )

def txtFile = basedir.resolve( 'count.txt' )
println "Write ${txtFile}"
Files.write( txtFile, pom.properties.manipulatorCount.toString().getBytes( StandardCharsets.UTF_8.name() ), StandardOpenOption.CREATE )
