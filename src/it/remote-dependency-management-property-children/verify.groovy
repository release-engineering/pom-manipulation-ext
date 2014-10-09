def pomFile = new File( basedir, 'pom.xml' )
def pomChildFile = new File( basedir, 'child/pom.xml' )

System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()} and ${pomChildFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def pomChild = new XmlSlurper().parse( pomChildFile )


def dependency = pom.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert dependency != null
assert dependency.version.text() != "4.1"
def failed = false

pom.properties.each {
    if ( it.text().contains ("3.8.2") )
    {
        failed = true
    }
}
assert (failed == false)

def childDependency = pomChild.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert childDependency != null
assert childDependency.version.text() != "4.1"

pomChild.properties.each {
    if ( it.text().contains ("3.8.2") )
    {
        failed = true
    }
}
assert (failed == false)
