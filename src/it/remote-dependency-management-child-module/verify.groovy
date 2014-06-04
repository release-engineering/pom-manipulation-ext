
def pomFile = new File( basedir, 'pom.xml' )
def pomChildFile = new File( basedir, 'child/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()} and ${pomChildFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def pomChild = new XmlSlurper().parse( pomChildFile )

def dependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-lang" }
assert dependency != null
assert dependency.version.text() == "2.5"

// Test overrideTransitive=false
def junitDependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert junitDependency.size() == 0

def childDependency = pomChild.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert childDependency != null
assert childDependency.version.text() == "4.1"
