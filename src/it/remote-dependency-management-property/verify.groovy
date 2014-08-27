def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

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
