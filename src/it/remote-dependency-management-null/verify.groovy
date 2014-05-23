def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

// Verify the junit version dependency hasn't changed.
def dependency = pom.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert dependency != null
assert dependency.version.text() == "3.8.2"

// An exception is thrown if there aren't any dependencies. Search the build.log for the exception.
def buildLog = new File( basedir, 'build.log' )
def exceptionRaised = false
buildLog.eachLine {
   if (it.contains( "Modification failed during project pre-scanning phase: Attempting to align to a BOM that does not have a dependencyManagement section")) {
      exceptionRaised = true
   }
}

assert exceptionRaised == true