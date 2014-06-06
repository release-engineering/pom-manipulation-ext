def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

pom.dependencyManagement.dependencies.each {
    if (it.artifactId.text() == "junit" && it.classifier.text() != "")
    {
        assert it.artifactId.text() == "sources"
    }

    if (it.artifactId.text() == "commons-lang" && it.classifier.text() != "")
    {
        assert it.artifactId.text() == "sources"
    }
}

return true