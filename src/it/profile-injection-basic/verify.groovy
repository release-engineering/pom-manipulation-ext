
def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

// We should have two profiles after injection
assert pom.profiles.children().size()

// Check the 3.8 version of junit has been overridden
def junit = pom.depthFirst().findAll { it.name() == 'version.junit' }
assert junit[0] == '4.1'
