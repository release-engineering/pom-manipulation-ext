
def pomFile = new File( basedir, 'pom.xml' )
def pomChildFile = new File( basedir, 'child/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()} and ${pomChildFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def pomChild = new XmlSlurper().parse( pomChildFile )

// As the child is 'standalone' it should have matching modified
// properties and dependencies.
assert pom.properties == pomChild.properties
assert pom.dependencyManagement.dependencies == pomChild.dependencyManagement.dependencies
