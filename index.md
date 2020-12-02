---
---

* Contents
{:toc}

### Overview

POM Manipulation Extension (PME) is a Maven tool to align the versions in your POMs according to some external reference, sort of like a BOM but much more extensive and without the added baggage of a BOM declaration.

It is suppplied as a core library, a Maven extension (in the sense of installing to `lib/ext`, not `pom.xml` `<extensions/>`) and a command line tool.

PME excels in a cleanroom environment where large numbers of pre-existing projects must be rebuilt. To minimize the number of builds necessary, PME supports aligning dependency versions using an external BOM-like reference. However, it can also use a similar POM external reference to align plugin versions, and inject standardized plugin executions into project builds. Because in this scenario you're often rebuilding projects from existing release tags, PME also supports appending a rebuild version suffix, such as `rebuild-1`, where the actual rebuild number is automatically incremented beyond the highest rebuild number detected in the Maven repository.

### Release Notes

For a list of changes please see [here](https://github.com/release-engineering/pom-manipulation-ext/releases)

### Installation

#### Installation as CLI tool.

Obtain the jar from [here](https://repo1.maven.org/maven2/org/commonjava/maven/ext/pom-manipulation-cli) and then it may be invoked as

    java -jar pom-manipulation-cli-<version>.jar

It supports the following arguments

    -D <arg>                  Java Properties
    -f,--file <arg>           POM file
    -h,--help                 Print help
    -l,--log <arg>            Log file to output logging to
    -P,--activeProfiles <arg> Comma separated list of active profiles.
    -s,--settings <arg>       Optional settings.xml file
    -q,--quiet                Enable quiet
    -d,--debug                Enable debug
    -t,--trace                Enable trace
    -p,--printDeps            Print all project dependencies
    --printManipulatorOrder   Print the current manipulator order

**Note:**
* All property arguments are the equivalent to those used when it is used as a Maven extension.
* The latter two commands do not run the entire tool.
* Log file output will be disabled if running inside a container image.


#### Installation as an Extension

Installing PME is as simple as [grabbing the binary](https://repo1.maven.org/maven2/org/commonjava/maven/ext/pom-manipulation-ext) and copying it to your `${MAVEN_HOME}/lib/ext` directory. Once PME is installed, Maven should output something like the following when run:

	[INFO] Maven-Manipulation-Extension

Uninstalling the extension is equally simple: just delete it from `${MAVEN_HOME}/lib/ext`.

### Disabling the Extension

You can disable PME using the `manipulation.disable` property:

	$ mvn -Dmanipulation.disable=true clean install

If you want to make it more permanent, you could add it to your `settings.xml`:

    <settings>
        <profiles>
            <profile>
                <id>disable-pme</id>
                <properties>
                    <manipulation.disable>true</manipulation.disable>
                </properties>
            </profile>
        </profiles>
        <activeProfiles>
            <activeProfile>disable-pme</activeProfile>
        </activeProfiles>
    </settings>

### Deprecated and Unknown Properties

<table bgcolor="#ffff00">
<tr>
<td>
    <b>NOTE</b> : Available from version 4.0
</td>
</tr>
</table>

If an unknown property is passed in the tool (whether in CLI or extension mode) will output a warning with the unknown property.

By default if a deprecated property is used the tool will detect it and throw an exception. If the user wishes to allow usage of deprecated properties set `enabledDeprecatedProperties` to true.

### Extension Marker File

When the extension runs it writes out a control marker file in the execution root `target` directory. This is named `pom-manip-ext-marker.txt`. If this marker file exists PME will not run a second time instead logging:

    Skipping manipulation as previous execution found.

Removing the target directory will allow PME to be run again.

### Summary Logging

PME will output a summary of its changes at the end of the run. As well as reporting version, property, dependency and plugin alignment it is also possible to report what _hasn't_ been aligned by setting the property `reportNonAligned=true`. This summary may also be output to a file if `reportTxtOutputFile` is set.

Finally it will also output the comparator summary as a JSON file. By default this report will be placed within the execution root `target` directory (next to the marker file above). However it may be configured by setting `reportJSONOutputFile`.

    {
      "executionRoot" : {
        "groupId" : "org.foo",
        "artifactId" : "foo-parent",
        "version" : "7.0.0.Final-rebuild-1",
        "originalGAV" : "org.foo:foo-parent:7.0.0.Final"
      },
      "modules" : [ {
        "gav" : {
          "groupId" : "org.foo",
          "artifactId" : "foo-parent",
          "version" : "7.0.0.Final-rebuild-1",
          "originalGAV" : "org.foo:foo-parent:7.0.0.Final"
        },
        "properties" : {
        ...

This JSON file may be read as POJO by using the [JSONUtils](https://github.com/release-engineering/pom-manipulation-ext/blob/master/common/src/main/java/org/commonjava/maven/ext/common/util/JSONUtils.java) class which utilises the [json](https://github.com/release-engineering/pom-manipulation-ext/blob/master/common/src/main/java/org/commonjava/maven/ext/common/json) package.

### Javadoc

This is available [here](https://www.javadoc.io/doc/org.commonjava.maven.ext)

### Feature Guide

Below are links to more specific information about configuring sets of features in PME:

* [Configuration Files](guide/configuration.html)
* [Project version manipulation](guide/project-version-manip.html)
* [Dependency manipulation](guide/dep-manip.html)
* [Plugin manipulation](guide/plugin-manip.html)
* [Properties, Profiles, Repositories, Reporting, Etc.](guide/misc.html)
* [JSON manipulation](guide/json.html)
* [XML manipulation](guide/xml.html)
* [Groovy Script Injection](guide/groovy.html)
* [Index](guide/property-index.html)
