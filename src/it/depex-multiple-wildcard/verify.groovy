def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert dependency != null
assert dependency.version.text() != "4.11"

pomFile = new File( basedir, 'child1/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

pom = new XmlSlurper().parse( pomFile )

dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert dependency != null
assert dependency.version.text() != "4.11"

dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "logback-classic" }
assert dependency != null
assert dependency.version.text() == "1.0.12"
