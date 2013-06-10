localRepo = new File( '@localRepositoryUrl@' )

System.out.println( "Checking parent output..." )

def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def v = pom.version.text()
def g = pom.groupId.text()
def a = pom.artifactId.text()

System.out.println( "POM Version: ${v}" )
assert v.endsWith( '.redhat-2' )

def repodir = new File( localRepo, "${g.replace('.', '/')}/${a}/${v}" )
def repopom = new File( repodir, "${a}-${v}.pom" )
System.out.println( "Checking for installed pom: ${repopom.getAbsolutePath()}")
assert repopom.exists()  

def managedDeps = pom.dependencyManagement.dependencies.children
managedDeps.each {
  System.out.println( "Checking managed dependency: ${it.groupId.text()}:${it.artifactId.text()}:${it.version.text()}" )
  assert it.version.text().endsWith('.redhat-2')
}

def children = ['child1', 'child2']
children.each() {
  System.out.println( "Checking output for ${it}..." )

  pomFile = new File( basedir, "${it}/pom.xml" )
  System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

  pom = new XmlSlurper().parse( pomFile )
  assert !pom.groupId.text()
  assert !pom.version.text()

  v = pom.parent.version.text()
  g = pom.parent.groupId.text()
  a = pom.artifactId.text()

  System.out.println( "POM Version: ${v}" )
  assert v.endsWith( '.redhat-2' )

  def jar = new File(basedir, "${it}/target/${a}-${v}.jar" )
  System.out.println( "Checking for jar: ${jar.getAbsolutePath()}")
  assert jar.exists()  

  repodir = new File(localRepo, "${g.replace('.', '/')}/${a}/${v}" )

  repopom = new File( repodir, "${a}-${v}.pom" )
  System.out.println( "Checking for installed pom: ${repopom.getAbsolutePath()}")
  assert repopom.exists()  

  def repojar = new File( repodir, "${a}-${v}.jar" )
  System.out.println( "Checking for installed jar: ${repojar.getAbsolutePath()}")
  assert repojar.exists()  
  
  if ( it == 'child2' ) {
    assert !pom.dependencies.dependency[0].version.text()
  }
}

return true