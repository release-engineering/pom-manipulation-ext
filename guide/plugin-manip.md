---
title: "Plugin Manipulation"
---

### Overview

PME can align plugin versions and configuration using a similar pattern to [dependencies](dep-manip.html). It also has the ability to standardize the use of `skip` flags that determine whether the `maven-install-plugin` and `maven-deploy-plugin` execute. Finally, by default PME will inject plugin executions for the `project-sources-maven-plugin` and `buildmetadata-maven-plugin`, in order to promote reproducibility of the project build.

### Basic Plugin Alignment

A remote plugin management POM is used to specify the plugin versions (and configuration) to inject:

    mvn install -DpluginManagement=org.jboss:jboss-parent:10

This will inject all `<pluginManagement/>` versions, executions and configuration from the remote POM into the local POM. As with [dependency management](dep-manip.html), multiple remote plugin management POMs can be specified on the command line using a comma separated list of GAVs.  The first POM specified will be given the highest priority if conflicts occur.

    mvn install -DpluginManagement=org.company:pluginMgrA:1.0,org.company:pluginMgrB:2.0

By default the extension will inject _all_ plugins declared in the remote BOM. If the option `overrideTransitive` is set to `false` to then only plugins used will be overridden.

    mvn install -DpluginManagement=org.foo:my-dep-pom:1.0 -DoverrideTransitive=false

**Note**: overrideTransitive is also used by the Dependency Manipulator.

If there is an existing local configuration then it will be merged with the remote. The following configuration controls the precedence:

    -DpluginManagementPrecedence=[LOCAL|REMOTE]

Default is `REMOTE` which means the remote configuration takes precedence over local.

If when attempting to merge the remote execution blocks into local, the `<id>`'s clash an exception will be thrown.

By default (unless `injectRemotePlugins` is set to false), PME will also inject any `<plugin/>` that have execution or configuration sections found in the remote BOM.

### Install and Deploy Skip Flag Alignment

By default, this extension will disable the skip flag on the install and deploy plugins. This is useful for build environments that compare the results of install with those from deploy as a validation step. More generally, suppressing installation or deployment tends to be an aesthetic decision that can have subtle functional consequences. It's usually not really worth the hassle.

This feature does support four modes for alignment, controlled via the **enforce-skip** command-line property:

1. `none` - (**default**) don't do any alignment
2. `on` - (aliased to `true`) enforce that the skip flag is **enabled**, suppressing install and deploy functions of the build (useful mainly for module-specific overrides. See below)
3. `off` - (aliased to `false`) enforce that the skip flag is **disabled** and that install/deploy functions will execute normally
4. `detect` - detect the flag state of the install plugin in the main pom, and adjust *any* other install- or deploy-plugin references to make their skip flag values consistent. Install plugin declarations in profiles are not considered during detection.

Additionally, the feature supports per-module overrides, which can be specified as:

    -DenforceSkip.org.group.id:artifact-id=(none|on|true|off|false|detect)

### Plugin Removal

If the property `-Dplugin-removal=group:artifact,....` is set, PME will remove the specified plugins from the POM files. The argument should be a comma separated list of group:artifact.

### Project Sources / Build Metadata Plugin Injection

The extension will inject an execution of [project-sources-maven-plugin](https://github.com/commonjava/project-sources-maven-plugin) and [build-metadata-plugin](https://github.com/release-engineering/buildmetadata-maven-plugin) by default. This will result in an archive being created containing all project sources **after** this extension has made any modifications to the pom.xml's. The archive will only be created in the execution-root project, and will be attached for installation and deployment using the `project-sources` classifier. The metadata plugin will create a build.properties file containing information (e.g. the command line) on the invoked project. This will also be included in the archive tar.

**Note**: this manipulator will only be active by default if one or more other manipulators have been activated.

To skip injection of the sources and metadata plugins, you can use:

    mvn install -Dproject.src.skip=true
    mvn install -Dproject.meta.skip=true

If unspecified, default versions of the project sources and metadata plugins will be injected (currently, version 0.3 and 1.5.0 respectively). To gain more control over this injection, you can specify the versions for project sources and metadata plugins like this:

    mvn install -Dproject.src.version=x.y
    mvn install -Dproject.meta.version=x.y
