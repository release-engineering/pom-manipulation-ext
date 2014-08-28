def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def plugin = null
pom.build.plugins.children().each{
    if (it.artifactId == "project-sources-maven-plugin" ){
        plugin = it
        return true
    }
}

// using the default version, which should be 0.3
assert plugin.version == '0.3'
