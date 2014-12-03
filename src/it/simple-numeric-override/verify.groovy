def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
System.out.println( "POM Version: ${pom.version.text()}" )

assert pom.version.text().contains( '6.1.0.Final' )
assert pom.version.text().endsWith( '-redhat-1' )

def jar = new File(basedir, "target/${pom.artifactId.text()}-${pom.version.text()}.jar" )
System.out.println( "Checking for jar: ${jar.getAbsolutePath()}")
assert jar.exists()

def repodir = new File('@localRepositoryUrl@', "${pom.groupId.text().replace('.', '/')}/${pom.artifactId.text()}/${pom.version.text()}" )
def repojar = new File( repodir, "${pom.artifactId.text()}-${pom.version.text()}.jar" )
System.out.println( "Checking for installed jar: ${repojar.getAbsolutePath()}")
assert repojar.exists()

def repopom = new File( repodir, "${pom.artifactId.text()}-${pom.version.text()}.pom" )
System.out.println( "Checking for installed pom: ${repopom.getAbsolutePath()}")
assert repopom.exists()
