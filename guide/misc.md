---
title: Miscellaneous PME Manipulations
---

### Overview

In addition to the main [project-version](project-version-manip.html), [dependency](dep-manip.html), and [plugin](plugin-manip.html) manipulations, PME offers several smaller features. These feaures fall into two main categories:

* POM Cleanup
* Build Management

### POM Cleanup

#### Repository And Reporting Removal

If the property `-Drepo-reporting-removal=true` is set, PME will remove all reporting and repository sections (including profiles) from the POM files.

Repository declarations in the POM are considered a bad build smell, since over time they may become defunct or move.

If the property `-Drepo-removal-ignorelocalhost` is set (default: false) PME will not remove repositories that contain the following definitions

* file://
* (http or https)://localhost
* (http or https)://127.00.1
* (http or https)://::1

Occasionally a project's more complex example/quickstart may have a local repository definition; this allows those to be preserved.

Additionally, most project rebuilers aren't interested in hosting their own copy of the project's build reports or generated website; therefore, the reporting section only adds more plugin artifacts to the list of what must be present in the environment for the build to succeed. Eliminating this section simplifies the build and reduces the risk of failed builds.

#### `project.version` Expression Replacement

The extension will automatically replace occurences of the property expression `${project.version}` in POMs (of packaging type `pom`).

This avoids a subtle problem that occurs when another project with inherits from this POM. If the child POM (the one that declares the `<parent/>`) specifies its own version **and that version is different from the parent**, that child version will be used to resolve `${project.version}` instead of the intended (parent) version. Resolving these expressions when `packaging` is set to `pom` (the only type of POM that can act as a parent) prevents this from occurring.

This behavior may be configured by setting:

    -Denforce-project-version=on|off

As explained above, the default is `on`.

### Build Management

#### Profile Injection

PME supports injection of profiles declared in a remote POM file. Simply supply a remote management POM:

    mvn install -DprofileInjection=org.foo:profile-injection:1.0

The extension will, for every profile in the remote POM file, replace or add it to either the local top level POM file or the POM specified by the property `profileInjectionGA` (which should be in the form of e.g. `org.myproject:mychild`).

**Note:** for any existing profile in the modified POM that specifies `activeByDefault`, this activation option will be removed so profiles are not accidentally disabled due to its exclusive semantics.

#### Profile Removal

PME supports removal of profiles as indicated by a comma separated list of profile IDs.

    mvn install -DprofileRemoval=profileOne,profileTwo

#### Repository Injection

PME supports injection of remote repositories. Supply a remote repository management POM:

	mvn install -DrepositoryInjection=org.foo:repository-injection:1.0

The extension will resolve a remote POM file and inject remote repositories to the local top level POM file. If there is a local repository with id identical to the injected one, it is overwritten.

#### Property Override

PME may also be used to override properties prior to interpolating the model. Multiple property mappings can be overridden using a similar pattern to dependencies via a remote property management pom.

    mvn install -DpropertyManagement=org.foo:property-management:10

This will inject the properties at the inheritance root(s). It will also, for every injected property, find any matching property in the project and overwrite its value.

Overriding properties can be a simple, minimalist way of controlling build behavior if the appropriate properties are already defined.
