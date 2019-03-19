---
title: Groovy Script Injection
---

### Overview

PME offers the ability to run arbitrary groovy scripts on the sources prior to running the build. This allows PME to be extensible by the user and to process other files not just Maven POMs.
<table bgcolor="red">
<tr>
<td>
    <b>Warning : Do NOT alter POM files directly on the disk; they will get overwriten by the POM Manipulator. The Manipulator processes the POM files in memory and then writes them back out to disk. If you wish to alter the POM files access the <i>Project</i> class and call <i>getModel()</i> to retrieve the <i>org.apache.maven.model.Model instance.</i></b>
</td>
</tr>
</table>



### Configuration

If the property `-DgroovyScripts=<value>,....` is set, PME will load the remote Groovy script file.

The argument should a comma separated list of either:

* group:artifact:version (with optional type and classifiers).
* A HTTP / HTTPS URL.

If using a GAVTC, the remote groovy file can be deployed by e.g.

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

Each groovy script will be run on the execution root (i.e. where Maven is invoked).

<table bgcolor="#ffff00">
<tr>
<td>
    <b>NOTE</b> : Prior to PME 3.5 groovy manipulator precendence was controlled via a flag ; by setting <i>groovyManipulatorPrecedence</i> to <i>FIRST</i> instead of the default <i>LAST</i> value. Further, annotation was different; the scripts used the <a href="http://docs.groovy-lang.org/latest/html/gapi/groovy/transform/BaseScript.html">BaseScript</a> annotation e.g.
<br/>
<i>@BaseScript org.commonjava.maven.ext.core.groovy.BaseScript pme</i>
</td>
</tr>
</table>

<table bgcolor="#ffff00">
<tr>
<td>
    <b>NOTE</b> : As of PME 3.0 the API has changed: <i>org.commonjava.maven.ext.core.groovy.BaseScript</i> instead of <i>org.commonjava.maven.ext.manip.groovy.BaseScript</i> and <i>org.commonjava.maven.ext.common.model.Project</i> instead of <i>org.commonjava.maven.ext.manip.model.Project</i>
</td>
</tr>
</table>

Each script <b>must</b> use the following annotations:

```

import org.commonjava.maven.ext.core.groovy.PMEBaseScript
import org.commonjava.maven.ext.core.groovy.PMEInvocationPoint
...

@PMEInvocationPoint(invocationPoint = InvocationStage.FIRST)
@PMEBaseScript BaseScript pme
```

where InvocationStage may be `FIRST`, `LAST` or `BOTH`. This denotes whether the script is ran
before all other manipulators, after or both. The script therefore encodes how and when it is run.

<br/>
The following API is made available:


| Method | Description |
| -------|:------------|
| [Properties](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html) getUserProperties() | Get the user properties. |
| [File](https://docs.oracle.com/javase/7/docs/api/java/io/File.html) getBaseDir() | Get the working directory (the execution root). |
| [ProjectVersionRef](https://github.com/Commonjava/atlas/blob/master/identities/src/main/java/org/commonjava/maven/atlas/ident/ref/ProjectVersionRef.java) getGAV() | Obtain the GAV of the current project |
| [Project](https://github.com/release-engineering/pom-manipulation-ext/blob/master/common/src/main/java/org/commonjava/maven/ext/common/model/Project.java) getProject() | Return the current Project |
| [ArrayList](https://docs.oracle.com/javase/7/docs/api/java/util/ArrayList.html)<[ProjectVersionRef](https://github.com/Commonjava/atlas/blob/master/identities/src/main/java/org/commonjava/maven/atlas/ident/ref/ProjectVersionRef.java)> getProjects() | Returns the entire collection of Projects |
| [MavenSessionHandler](https://github.com/release-engineering/pom-manipulation-ext/blob/master/common/src/main/java/org/commonjava/maven/ext/common/session/MavenSessionHandler.java) getSession() | Return the current session handler |
| [ModelIO](https://github.com/release-engineering/pom-manipulation-ext/blob/master/io/src/main/java/org/commonjava/maven/ext/io/ModelIO.java) getModelIO() | Return a ModelIO instance for artifact resolving |
| [InvocationStage](https://github.com/release-engineering/pom-manipulation-ext/blob/master/core/src/main/java/org/commonjava/maven/ext/core/groovy/InvocationStage.java) getInvocationStage() | Return the current stage of the groovy manipulation. |

This can then be invoked by e.g.

    pme.getBaseDir()


A typical groovy script that alters a JSON file on disk might be:


    import groovy.json.JsonOutput
    import groovy.json.JsonSlurper
    import groovy.util.logging.Slf4j
    import org.commonjava.maven.ext.core.groovy.PMEBaseScript
    import org.commonjava.maven.ext.core.groovy.PMEInvocationPoint

    @PMEInvocationPoint(invocationPoint = InvocationStage.FIRST)
    @PMEBaseScript BaseScript pme

    @Slf4j
    public class Processor
    {
        File basedir

        private void processJson(Map n) {
            ....
        }

        def execute() {
            log.info("Running ShrinkwrapProcessor...")

            def shrinkwrap = new File (basedir.toString() + File.separator +
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

    def Processor sp = new Processor(basedir:pme.getBaseDir()))
    sp.execute()



If a developer wishes to setup an IDE to write the groovy script we would recommend adding to the POM file and activating this profile within the IDE:

    <!-- This profile is only used within IntelliJ for Groovy development -->
    <profiles>
      <profile>
        <id>groovy</id>
        <dependencies>
          <dependency>
            <groupId>org.commonjava.maven.ext</groupId>
            <artifactId>pom-manipulation-core</artifactId>
            <version>1.13</version>
            <scope>provided</scope>
          </dependency>
        </dependencies>
      </profile>
    </profiles>
