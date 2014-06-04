def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

// An exception is thrown if there aren't any dependencies. Search the build.log for the exception.
def buildLog = new File( basedir, 'build.log' )
def exceptionRaised = false
buildLog.eachLine {
   if (it.contains( "POM Manipulation failed")) {
      exceptionRaised = true
   }
}

assert exceptionRaised == true
