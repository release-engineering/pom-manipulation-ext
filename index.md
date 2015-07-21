---
---

### Overview

POM Manipulation Extension (PME) is a Maven extension (in the sense of installing to `lib/ext`, not `pom.xml` `<extensions/>`). Its purpose is to align the versions in your POMs according to some external reference, sort of like a BOM but much more extensive and without the added baggage of a BOM declaration. 

PME excels in a cleanroom environment where large numbers of pre-existing projects must be rebuilt. To minimize the number of builds necessary, PME supports aligning dependency versions using an external BOM-like reference. However, it can also use a similar POM external reference to align plugin versions, and inject standardized plugin executions into project builds. Because in this scenario you're often rebuilding projects from existing release tags, PME also supports appending a rebuild version suffix, such as `rebuild-1`, where the actual rebuild number is automatically incremented beyond the highest rebuild number detected in the Maven repository.

### Installation

Installing PME is as simple as [grabbing the binary](http://central.maven.org/maven2/org/commonjava/maven/ext/pom-manipulation-ext) and copying it to your `${MAVEN_HOME}/lib/ext` directory. Once PME is installed, Maven should output something like the following when run:

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

### Feature Guide

Below are links to more specific information about configuring sets of features in PME:

* [Project version manipulation](guide/project-version-manip.html)
* [Dependency manipulation](guide/dep-manip.html)
* [Plugin manipulation](guide/plugin-manip.html)
* [Properties, Profiles, Repositories, Reporting, Etc.](guide/misc.html)
