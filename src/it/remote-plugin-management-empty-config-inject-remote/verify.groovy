def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def plugin = pom.build.pluginManagement.plugins.plugin.find { it.artifactId.text() == "maven-compiler-plugin" }
assert plugin != null
assert plugin.version.text() == "3.1"

plugin = pom.build.pluginManagement.plugins.plugin.find { it.artifactId.text() == "maven-compiler-plugin" }
assert plugin != null
assert plugin.version.text() == "3.1"

def message = 0
pomFile.eachLine {
   if (it.contains( "<debug>true</debug>")) {
      message++
   }
   if (it.contains( "maven-compiler-plugin")) {
      message++
   }
}

assert message == 2
