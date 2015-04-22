def pomFile = new File( basedir, 'pom.xml' )
System.out.println( "Slurping POM: ${pomFile.getAbsolutePath()}" )

def pom = new XmlSlurper().parse( pomFile )

def plugin = null
pom.build.plugins.children().each{
    if (it.artifactId == "project-sources-maven-plugin" ){
        assert it.version == '0.3'
    }
    else if (it.artifactId == 'build-metadata-maven-plugin'){
        assert it.version == '1.3.1'
    }
}

assert new File( basedir, 'build.metadata' ).exists()
assert new File( basedir, 'target/project-sources-injection-basic-1-project-sources.tar.gz').exists()

return true

