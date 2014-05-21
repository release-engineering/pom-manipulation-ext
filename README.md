# POM Manipulation Extension for Apache Maven

A Maven extension which provides a series of POM pre-processors. When it is activated it will write a log file `target/manipulation.log`.

This extension combines many of the functionality of [VMan](https://github.com/jdcasey/pom-version-manipulator), [Maven Versioning Extension](https://github.com/jdcasey/maven-versioning-extension) and [Maven Dependency Management Extension](https://github.com/jboss/maven-dependency-management-extension).

## Installation

The extension jar can be downloaded from the GitHub Releases page here (TODO) or from a Maven repository (TODO), or it can be built from source. The jar should be added to the directory `${MAVEN_HOME}/lib/ext`.  The next time Maven is started, you should see a command line message showing that the extension has been installed.

    [INFO] Maven-Manipulation-Extension

If you wish to remove the extension it must be physically deleted from the `lib/ext` directory. Alternatively it may be disabled globally.

### Global disable flag

To disable the entire extension, you can set:

    mvn install -Dmanipulation.disable=true


## Version manipulation

The following version-related configuration is available:

### Automatic version increment

The extension can be used to append a version suffix/qualifier
to the current project, and then apply an incremented index to the version
to provide a unique release version.  For example, if the current
project version is 1.0.0.GA, the extension can automatically set the version
to 1.0.0.GA-rebuild-1, 1.0.0.GA-rebuild-2, etc.

The extension is configured using the property **version.incremental.suffix**.

    mvn install -Dversion.incremental.suffix=rebuild

The Maven repository metadata will be checked to locate the latest released version of the project artifacts, and the next version is selected by the extension.

### Manual version suffix

The version suffix to be appended to the current project can be manually selected using the property **version.suffix**

    mvn install -Dversion.suffix=release-1

If the current version of the project is "1.2.0.GA", the new version set during the build will be "1.2.0.GA-release-1".

### Snapshot Detection

The extension can detect snapshot versions and either preserve the snapshot or replace it with a real version. This is controlled by the property **version.suffix.snapshot**. The default is false (i.e. remove SNAPSHOT and replace by the suffix).

    mvn install -Dversion.suffix.snapshot=true

This means that the SNAPSHOT suffix will be kept.

## Repository And Reporting Removal

If the property **repo-reporting-removal** is set to true then reporting and repository sections will be removed from the POM files.

## Dependency Manipulation

The extension can override a set of dependencyVersions using a remote pom (BOM) file. By default , all dependencies listed in the remote pom will be added to the current build. This has the effect of overriding matching transitive dependencies, as well as those specified directly in the pom.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0

Multiple remote dependency management poms can be specified using a comma separated list of GAVs (groupId, artifactId, version). The poms are specified in order of priority, so if the remote boms contain some of the same dependencies,
the versions listed in the first bom in the list will be used.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0,org.bar:my-dep-pom:2.0

If transitive dependencies should not be overridden, the option "overrideTransitive" can be set to false.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0 -DoverrideTransitive=false

### Exclusions and Per Module Overrides

In a multi-module build it is considered good practice to coordinate dependency version among the modules using dependency management.  In other words, if module A and B both use dependency X, both modules should use the same version of dependency X.  Therefore, the default behaviour of this extension is to use a single set of dependency versions applied to all modules.

However, there are certain cases where it is useful to use different versions of the same dependency in different modules.  For example, if the project includes integration code for multiple versions of a particular API. In that case it is possible to apply a version override to a specific module of a multi-module build.

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=[version]

For example to apply a dependency override only to module B of project foo.

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=4.10


It is also possible to prevent overriding dependency versions on a per module basis:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=

For example

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=

It is also possible to prevent overriding a dependency version across the entire project using a wildcard:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@*=

For example

    mvn install -DdependencyExclusion.junit:junit@*=


### Using Dependency Properties

The extension will automatically set properties which match the version overrides.  These properties can be used, for example, in resource filtering in the build.  By default the extension supports two different formats for the properties. It is controlled by the property:

    -DversionPropertyFormat=[VG|VGA]

Where `VG` is `version.<group>` e.g. `version.org.slf4j` and `VGA` is `version.<group>.<artifact>`. The default is `VG`.

## Plugin Manipulation

The extension can also override plugin versions using a similar pattern to dependencies. A remote plugin management pom can be specified.

    mvn install -DpluginManagement=org.jboss:jboss-parent:10

This will apply all &lt;pluginManagement/&gt; versions from the remote pom, to the local pom.
Multiple remote plugin management poms can be specified on the command line using a comma separated
list of GAVs.  The first pom specified will be given the highest priority if conflicts occur.

    mvn install -DpluginManagement=org.company:pluginMgrA:1.0,org.company:pluginMgrB:2.0


## Property Override

The extension may also be used to override properties prior to interpolating the model. Multiple property mappings can be overridden using a similar pattern to dependencies via a remote property management pom.

    mvn install -DpropertyManagement=org.foo:property-management:10

Properties may be overridden on the command line as per normal Maven usage (i.e. -Dversion.org.foo=1.0)
