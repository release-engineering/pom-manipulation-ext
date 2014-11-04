
# POM Manipulation Extension for Apache Maven

A Maven extension which provides a series of POM pre-processors. When it is activated it will write a log file `target/manipulation.log`.

This extension combines many of the features of [VMan](https://github.com/jdcasey/pom-version-manipulator), [Maven Versioning Extension](https://github.com/jdcasey/maven-versioning-extension) and [Maven Dependency Management Extension](https://github.com/jboss/maven-dependency-management-extension).

## Contents

[//]: # (To regenerate the contents use "https://gist.github.com/ttscoff/c56fa651974ae6d86eee#file-github_toc-rb 4 README.md README.md")

- [Installation](#installation)
    - [Global disable flag](#global-disable-flag)
- [Version manipulation](#version-manipulation)
    - [Automatic version increment](#automatic-version-increment)
    - [Manual version suffix](#manual-version-suffix)
    - [Snapshot Detection](#snapshot-detection)
- [Repository And Reporting Removal](#repository-and-reporting-removal)
- [Dependency Manipulation](#dependency-manipulation)
    - [Direct Dependencies](#direct-dependencies)
    - [Direct/Transitive Dependencies](#directtransitive-dependencies)
    - [Exclusions and Per Module Overrides](#exclusions-and-per-module-overrides)
    - [Strict Mode Version Alignment](#strict-mode-version-alignment)
    - [Dependency Property Injection](#dependency-property-injection)
- [Plugin Manipulation](#plugin-manipulation)
- [Property Override](#property-override)
- [Profile Injection](#profile-injection)
- [Install and Deploy Skip Flag Alignment](#install-and-deploy-skip-flag-alignment)
- [Project Sources Plugin Injection](#project-sources-plugin-injection)

<!-- end toc -->

## Installation

The extension jar can be downloaded from the GitHub Releases page [here](https://github.com/jdcasey/pom-manipulation-ext/releases) or from Maven Central [here](http://central.maven.org/maven2/org/commonjava/maven/ext/pom-manipulation-ext), or it can be built from source. The jar should be added to the directory `${MAVEN_HOME}/lib/ext`. The next time Maven is started, you should see a command line message showing that the extension has been installed.

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

The extension will change any parent reference it finds that matches an entry in the remote BOM i.e.
```
      <parent>
         <groupId>org.switchyard</groupId>
         <artifactId>switchyard-parent</artifactId>
         <version>2.0.0.Alpha1</version>
```
will change to
```
      <parent>
         <groupId>org.switchyard</groupId>
         <artifactId>switchyard-parent</artifactId>
         <version>2.0.0.Alpha1-rebuild-1</version>
```


:large_blue_circle: Note that for existing dependencies that reference a property the tool will update
this property with the new version. If the property can't be found (e.g. it was inherited), a new one will
be injected at the top level. This update of the property's value **may** implicitly align other dependencies using the same property that were not explicitly requested to be aligned.

### Direct Dependencies

By default the extension will override dependencies from the remote BOM. This may be disabled via

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0 -DoverrideDependencies=false

Note that this will still alter any external parent references.

### Direct/Transitive Dependencies

By default the extension will inject all dependencies from the remote BOM. This will also override dependencies that are not directly specified in the project. If these transitive dependencies should not be overridden, the option "overrideTransitive" can be set to false.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0 -DoverrideTransitive=false

### Exclusions and Per Module Overrides

In a multi-module build it is considered good practice to coordinate dependency version among the modules using dependency management.  In other words, if module A and B both use dependency X, both modules should use the same version of dependency X.  Therefore, the default behaviour of this extension is to use a single set of dependency versions applied to all modules.

However, there are certain cases where it is useful to use different versions of the same dependency in different modules.  For example, if the project includes integration code for multiple versions of a particular API. In that case it is possible to apply a version override to a specific module of a multi-module build.

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=[version]

For example to apply an explicit dependency override only to module B of project foo.

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=4.10


It is also possible to prevent overriding dependency versions on a per module basis:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=

For example

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=

It is also possible to prevent overriding a dependency version across the entire project using a wildcard:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@*=

For example

    mvn install -DdependencyExclusion.junit:junit@*=

### Strict Mode Version Alignment

When aligning dependency versions to some shared standard, it's possible to introduce incompatibilities that stem from too large of a version change. For instance, while it might be safe to revise a dependency's version from 1.5 to 1.5.1, it may not be safe to revise it to 2.0, or even 1.6.

In cases where this is a concern, and for dependencies whose versions are aligned via a BOM (not via explicit overrides, as explained above), strict-mode version alignment checks can be enabled using:

    mvn install -DstrictAlignment=true

This will detect cases where the adjusted version doesn't start with the old version (i.e. 1.0 -> 1.0.1), **not** do the alignment and report the mismatch as a warning in the build's console output.

If, instead, the build should fail when strict-mode checks are violated, add the `strictViolationFails=true` property:

    mvn install -DstrictAlignment=true -DstrictViolationFails=true

This will cause the build to fail with a ManipulationException, and prevent the extension from rewriting any POM files.

:large_blue_circle: Note that for dependency exclusions they will not work if the dependency uses a version property that has been changed by another dependency modification. Explicit version override will overwrite the property value though.

### Dependency Property Injection

:warning: *This functionality is now deprecated ; if it is not required it may be removed in a future release.*

The extension will automatically set properties which match the version overrides.  These properties can be used, for example, in resource filtering in the build.  By default the extension supports two different formats for the properties. It is controlled by the property:

    -DversionPropertyFormat=[VG|VGA|NONE]

Where `VG` is `version.<group>` e.g. `version.org.slf4j`, `VGA` is `version.<group>.<artifact>` and `NONE` disables the injection. The default is `NONE`.


## Plugin Manipulation

The extension can also align plugin versions and configuration using a similar pattern to dependencies. A remote plugin management pom can be specified.

    mvn install -DpluginManagement=org.jboss:jboss-parent:10

This will apply all &lt;pluginManagement/&gt; versions and configuratoin from the remote pom, to the local pom. Multiple remote plugin management poms can be specified on the command line using a comma separated
list of GAVs.  The first pom specified will be given the highest priority if conflicts occur.

    mvn install -DpluginManagement=org.company:pluginMgrA:1.0,org.company:pluginMgrB:2.0

If there is an existing local configuration then it will be merged with the remote. The following configuration controls the precedence

    -DpluginManagementPrecedence=[LOCAL|REMOTE]

Default is `REMOTE` which means the remote configuration takes precedence over local.

## Property Override

The extension may also be used to override properties prior to interpolating the model. Multiple property mappings can be overridden using a similar pattern to dependencies via a remote property management pom.

    mvn install -DpropertyManagement=org.foo:property-management:10

This will inject the properties at the inheritance root(s). It will also, for every injected property, find any matching property in the project and overwrite its value.

## Profile Injection

The extension also supports generic profile injection using a remote pom file. By supplying a remote management pom e.g.

    mvn install -DprofileInjection=org.foo:profile-injection:1.0

The extension will, for every profile in the remote pom file, replace or add it to the local top level pom file.

**Note:** for existing profiles in the modified pom that have activeByDefault profile activation, that will get replaced so that the profiles are not accidentally disabled due to the semantics of activeByDefault.

## Install and Deploy Skip Flag Alignment

By default, this extension will disable the skip flag on the install and deploy plugins. This is useful for build environments that compare the results of install with those from deploy as a validation step. More generally, suppressing installation or deployment tends to be an aesthetic decision that can have subtle functional consequences. It's usually not really worth the hassle.

This feature does support four modes for alignment, controlled via the **enforce-skip** command-line property:

1. **none** - don't do any alignment
2. **on** - (aliased to **true**) enforce that the skip flag is **enabled**, suppressing install and deploy functions of the build (useful mainly for module-specific overrides. See below)
3. **off** - (aliased to **false**) (*default*) enforce that the skip flag is **disabled** and that install/deploy functions will execute normally
4. **detect** - detect the flag state of the install plugin in the main pom (not in profiles), and adjust *any* other install- or deploy-plugin references to the skip flag to be consistent.

Additionally, the feature supports per-module overrides, which can be specified as:

    -DenforceSkip.org.group.id:artifact-id=(none|on|true|off|false|detect)

## Project Sources Plugin Injection

The extension will inject an execution of [project-sources-maven-plugin](https://github.com/jdcasey/project-sources-maven-plugin) and [build-metadata-plugin](https://github.com/sbadakhc/buildmetadata-maven-plugin) by default. This will result in an archive being created containing all project sources **after** this extension has made any modifications to the pom.xml's. The archive will only be created in the execution-root project, and will be attached for installation and deployment using the `project-sources` classifier. The metadata plugin will create a build.properties file containing information (e.g. the command line) on the invoked project. This will also be included in the archive tar.

**Note**: this manipulator will only be active by default if one or more other manipulators have been activated.

To skip injection of the sources and metadata plugins, you can use:

    mvn install -Dproject.src.skip=true
    mvn install -Dproject.meta.skip=true

If unspecified, default versions of the project sources and metadata plugins will be injected (currently, version 0.3 and 1.5.0 respectively). To gain more control over this injection, you can specify the versions for project sources and metadata plugins like this:

    mvn install -Dproject.src.version=x.y
    mvn install -Dproject.meta.version=x.y
