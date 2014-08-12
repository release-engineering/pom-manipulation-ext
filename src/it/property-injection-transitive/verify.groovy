def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )
System.out.println(pom.properties);

def failed = false
pom.properties.each {
    if ( it.text().contains ("2.5") )
    {
        failed = true
    }
}

assert (failed == false)
