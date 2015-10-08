---
title: "Project Version Manipulation"
---

### Overview

When rebuilding a Maven project's sources from a release tag (or really for any version that has already been released), it's important **NOT** to republish the original GAV (groupId, artifactId, version) coordinate. If you change anything during your rebuild (even a plugin version), you could produce a result that is not a binary equivalent of the original release. To help avoid this, PME supports automatically updating the project version to append a serial rebuild suffix. PME's re-versioning feature may also make other changes to the project version, in order to make the resulting version OSGi-compliant where possible.

PME offers the following version-related configuration:

### Automatic version increment

The extension can be used to append a version suffix/qualifier to the current project, and then apply an incremented index to the version to provide a unique release version.  For example, if the current project version is 1.0.0.GA, the extension can automatically set the version to 1.0.0.GA-rebuild-1, 1.0.0.GA-rebuild-2, etc.

The extension is configured using the property `version.incremental.suffix`.

    mvn install -Dversion.incremental.suffix=rebuild

#### Version Increment Metadata

The metadata to work out what the correct version of the increment should be can be sourced the one of two different locations.

1. By default the Maven repository metadata will be checked to locate the latest released version of the project artifacts, and the next version is selected by the extension.

2. Alternatively if the property `-DrestURL` has been configured the the REST client service will be used as a source. For more details on this see Dependency Manipulation. See [here](./dep-manip.html) for more details on the REST endpoint.

### Manual version suffix

The version suffix to be appended to the current project can be manually selected using the property `version.suffix`

    mvn install -Dversion.suffix=release-1

If the current version of the project is "1.2.0.GA", the new version set during the build will be "1.2.0.GA-release-1".

### Version override

The version can be forcibly overridden by using the property `version.override`

    mvn install -Dversion.override=6.1.0.Final

If the current version of the project is "6.2.0", the new version set during the build will be "6.1.0.Final". A combination of properties may be used e.g.

    mvn install -Dversion.override=6.1.0.Final -Dversion.suffix=rebuild-1

Using the above example, this would result in the version being "6.1.0.Final-rebuild-1".

### Snapshot Detection

The extension can detect snapshot versions and either preserve the snapshot or replace it with a real version. This is controlled by the property `version.suffix.snapshot`. The default is false (i.e. remove SNAPSHOT and replace by the suffix).

    mvn install -Dversion.suffix.snapshot=true

This means that the SNAPSHOT suffix will be kept.

### OSGi Compliance

If version manipulation is enabled the extension will also attempt to format the version to be OSGi compliant. For example if the versions are:

    1
    1.3
    1.3-GA
    1.3.0-GA

it will change to

    1.0.0
    1.3.0
    1.3.0.GA
    1.3.0.GA

This is controlled by the property `version.osgi`. The default is true (i.e. make the versions OSGi compliant).
