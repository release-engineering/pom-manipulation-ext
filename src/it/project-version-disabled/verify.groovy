def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
System.out.println( "POM Version: ${pom.version.text()}" )


dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-lang" }
assert dependency != null

boolean contains = (dependency.version.text().contains( 'project.version' ) )
assert contains == true

dependency = pom.profiles.profile.dependencies.dependency.find { it.artifactId.text() == "commons-io" }
assert dependency != null
contains = (dependency.version.text().contains( 'project.version' ) )
assert contains == true

dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-net" }
assert dependency != null
assert dependency.version.text() == "2.0"
