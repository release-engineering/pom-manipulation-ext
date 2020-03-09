/**
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
def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def failed = true
def plugin = null
pom.build.plugins.children().each{
    if (it.artifactId == "project-sources-maven-plugin" ){
        assert it.version == '1.0'
    }
    else if (it.artifactId == 'buildmetadata-maven-plugin'){
        failed = false
    }
}

assert failed == false

def zips = [ new File ( basedir, "/jar/target/build-metadata-plugin-jar-1.jar" ),
             new File ( basedir, "/bundle/target/build-metadata-plugin-bundle-1.jar" ),
             new File ( basedir, "/war/target/build-metadata-plugin-war-1.war" ),
             new File ( basedir, "/ear/target/build-metadata-plugin-ear-1.ear" )
]

for ( File target : zips )
{
    System.out.println ("Using " + target)
    def zipFile = new java.util.zip.ZipFile(target)
    failed = true
    zipFile.entries().each {
        println zipFile.getInputStream(it).text
        if ( zipFile.getInputStream(it).text.contains("buildmetadata-maven-plugin 1.7.0") )
        {
            failed = false
        }
    }
    if ( failed == true ) System.out.println ("### Failed with " + target)
    assert failed == false
}
assert new File( basedir, 'target/build-metadata-plugin-1-project-sources.tar.gz').exists()
