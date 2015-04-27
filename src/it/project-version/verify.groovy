def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
System.out.println( "POM Version: ${pom.version.text()}" )


dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-lang" }
assert dependency != null
assert dependency.version.text() != "project.version"
assert dependency.version.text() == "1.0.0.redhat-1"

dependency = pom.profiles.profile.dependencies.dependency.find { it.artifactId.text() == "commons-io" }
assert dependency != null
assert dependency.version.text() != "project.version"
assert dependency.version.text() == "1.0.0.redhat-1"

dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-net" }
assert dependency != null
assert dependency.version.text() == "2.0"
