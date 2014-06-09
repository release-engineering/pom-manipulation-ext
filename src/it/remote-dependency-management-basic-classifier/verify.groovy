def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
def counter = 0
def sources = 0

pom.dependencyManagement.dependencies.childNodes().each {
    counter++;

    if ( it.text().contains ("sources") )
    {
        sources++
    }
}

// Checks that 4 dependencies have been injected - 2 junit and 2 commons-lang
assert counter == 4
assert sources == 2
