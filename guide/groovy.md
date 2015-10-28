---
title: Groovy Script Injection
---

### Overview

PME offers the ability to run arbitrary groovy scripts on the sources prior to running the build. This allows PME to be extensible by the user and to process other files not just Maven POMs.

### Configuration

If the property `-DgroovyScripts=GAVTC,....` is set, PME will load the remote Groovy script file. The argument should be a comma separate list of group:artifact:version (with optional type and classifiers).

The remote groovy file can be deployed by e.g.

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>build-helper-maven-plugin</artifactId>
                <version>1.5</version>
                <inherited>false</inherited>
                <executions>
                    <execution>
                        <id>attach-artifacts</id>
                        <phase>package</phase>
                        <goals>
                            <goal>attach-artifact</goal>
                        </goals>
                        <configuration>
                            <artifacts>
                                <artifact>
                                    <file>Sample.groovy</file>
                                    <type>groovy</type>
                                </artifact>
                            </artifacts>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>

The deployed file can then be used with e.g.

    mvn -DgroovyScripts=org.commonjava.maven.ext:depMgmt1:groovy:1.0 clean install


### Groovy Scripts

The groovy script will be run once on the execution root (i.e. where Maven is invoked).
It will have the following properties injected into it:

| Property      | Value |
| ------------- |:-------------:|
| basedir       | Directory of the execution root |
| projects      | ArrayList of all [Projects](https://github.com/release-engineering/pom-manipulation-ext/blob/master/common/src/main/java/org/commonjava/maven/ext/manip/model/Project.java) |
| project       | Current [Project](https://github.com/release-engineering/pom-manipulation-ext/blob/master/common/src/main/java/org/commonjava/maven/ext/manip/model/Project.java)|
| name          | Current project GAV (a [ProjectVersionRef](https://github.com/Commonjava/atlas/blob/master/identities/src/main/java/org/commonjava/maven/atlas/ident/ref/ProjectVersionRef.java)) |


A typical groovy script might be:


    import groovy.json.JsonOutput
    import groovy.json.JsonSlurper
    import groovy.util.logging.Slf4j

    @Slf4j
    public class Processor
    {
        def binding

        private void processJson(Map n) {
            ....
        }

        def execute() {
            log.info("Running ShrinkwrapProcessor...")

            def shrinkwrap = new File (binding.variables.basedir +
                java.nio.file.FileSystems.getDefault().getSeparator() +
                "shrink.json")

            log.info("shrinkwrap json is " + shrinkwrap)

            if (shrinkwrap.exists()) {
                log.info ("Found file {}", shrinkwrap)

                LinkedHashMap json = new
                    JsonSlurper().parseText(shrinkwrap.text)

                processJson(json)

                shrinkwrap.write(
                    JsonOutput.prettyPrint(JsonOutput.toJson(json)))
            }
        }
    }

    def Processor sp = new Processor(binding:binding)
    sp.execute()
