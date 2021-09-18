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

@InvocationPoint( invocationPoint = InvocationStage.PREPARSE )
@PMEBaseScript BaseScript pme

assert pme.getBaseDir() != null
assert pme.getGAV() == null
assert pme.getProjects().isEmpty()

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

println 'Original version: ' + pom.version
assert pom.version == '1.0-SNAPSHOT'
pom.version = '2.0'
println 'New Version: ' + pom.version
assert pom.version == '2.0'

final def profileToRemove = pom.profiles.'**'.find { final profile ->
    profile.id.text() == 'remove'
}

assert profileToRemove != null

profileToRemove.replaceNode {

}


final def xml = XmlUtil.serialize( pom )
Files.write( pomFile, xml.getBytes( StandardCharsets.UTF_8.name() ), StandardOpenOption.TRUNCATE_EXISTING )
