
def pomFile = new File( basedir, 'pom.xml' )
def pomChildFile = new File( basedir, 'child/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()} and ${pomChildFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def pomChild = new XmlSlurper().parse( pomChildFile )

def commonsDependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-lang" }
assert commonsDependency != null
assert commonsDependency.version.text() == "2.5"

def junitDependency = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert junitDependency != null
assert junitDependency.version.text() == "3.8.2"

def childDependency = pomChild.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert childDependency != null
assert childDependency.version.text() == "3.8.2"
