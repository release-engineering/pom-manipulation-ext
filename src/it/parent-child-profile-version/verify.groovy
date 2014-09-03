
def pomFile = new File( basedir, 'pom.xml' )
def pomChildFile = new File( basedir, 'child1/pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()} and ${pomChildFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def pomChild = new XmlSlurper().parse( pomChildFile )

assert pom.version.text().endsWith( '.redhat-1' )
assert pomChild.version.text().endsWith( '.redhat-1' )
