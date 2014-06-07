def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def buildLog = new File( basedir, 'build.log' )
def message = 0
buildLog.eachLine {
   if (it.contains( "PropertyManipulator: Nothing to do")) {
      message++
   }
   if (it.contains( "ProfileInjectionManipulator: Nothing to do")) {
      message++
   }
   if (it.contains( "PluginManipulator: Nothing to do")) {
      message++
   }
}

assert message == 3
