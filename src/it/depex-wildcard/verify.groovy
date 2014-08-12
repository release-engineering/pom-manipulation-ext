def pomFile = new File( basedir, 'child1/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert dependency != null
assert dependency.version.text() == "4.10"

pomFile = new File( basedir, 'child2/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

pom = new XmlSlurper().parse( pomFile )

dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert dependency != null
assert dependency.version.text() == "4.11"
