def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
System.out.println( "POM Version: ${pom.version.text()}" )
System.out.println( "POM Profile Activation: ${pom.profiles.profile.activation.file.exists.text()}" )
System.out.println( "POM Profile Property: ${pom.profiles.profile.properties.pomFile.text()}" )

def depPath = pom.profiles.profile.dependencies.dependency.systemPath.text()
def actPath = pom.profiles.profile.activation.file.exists.text()

assert actPath.equals(depPath)
assert actPath.indexOf('basedir') > -1
assert depPath.indexOf('basedir') > -1
