def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def junitDep = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "junit" }
assert junitDep != null
assert junitDep.classifier.text() == "sources"

def commonsLangDep = pom.dependencyManagement.dependencies.dependency.find { it.artifactId.text() == "commons-lang" }
assert commonsLangDep != null
assert commonsLangDep.classifier.text() == "sources"
