---
title: "Project Version Manipulation"
---

* Contents
{:toc}

### Overview

When rebuilding a Maven project's sources from a release tag (or really for any version that has already been released), it's important **NOT** to republish the original GAV (groupId, artifactId, version) coordinate. If you change anything during your rebuild (even a plugin version), you could produce a result that is not a binary equivalent of the original release. To help avoid this, PME supports automatically updating the project version to append a serial rebuild suffix. PME's re-versioning feature may also make other changes to the project version, in order to make the resulting version OSGi-compliant where possible.

PME offers the following version-related configuration:

### Automatic version increment

The extension can be used to append a version suffix/qualifier to the current project, and then apply an incremented index to the version to provide a unique release version.  For example, if the current project version is 1.0.0.GA, the extension can automatically set the version to 1.0.0.GA-rebuild-1, 1.0.0.GA-rebuild-2, etc.

The extension is configured using the property `versionIncrementalSuffix` (*Deprecated property `version.incremental.suffix` for versions **3.8.1 and prior***).

    mvn install -DversionIncrementalSuffix=rebuild

#### Version increment padding

<table bgcolor="#ffff00">
<tr>
<td>
    <b>NOTE</b> : For PME versions <b>after</b> 3.8, the default for <i>versionIncrementalSuffixPadding</i> has changed from 0 to 5.
</td>
</tr>
</table>

When using the automatic increment it is also possible to configure padding for the increment. For instance, by setting `versionIncrementalSuffixPadding` (*Deprecated property `version.incremental.suffix.padding` for versions **3.8.1 and prior***)to `3` the version will be `rebuild-003`. Default for PME &le; 3.8 is 0 and for PME after 3.8 is 5.

#### Version Increment Metadata

The metadata to work out what the correct version of the increment should be can be sourced the one of two different locations.

1. By default the Maven repository metadata will be checked to locate the latest released version of the project artifacts, and the next version is selected by the extension.

2. Alternatively if the property `-DrestURL` has been configured the the REST client service will be used as a source. For more details on this see Dependency Manipulation. See [here](./dep-manip.html) for more details on the REST endpoint.

### Manual version suffix

The version suffix to be appended to the current project can be manually selected using the property `versionSuffix` (*Deprecated property `version.suffix` for versions **3.8.1 and prior***)

    mvn install -DversionSuffix=release-1

If the current version of the project is "1.2.0.GA", the new version set during the build will be "1.2.0.GA-release-1".

**Note** `versionSuffix` takes precedence over `versionIncrementalSuffix`.

### Version override

The version can be forcibly overridden by using the property `versionOverride` (*Deprecated property `version.override` for versions **3.8.1 and prior***)

    mvn install -DversionOverride=6.1.0.Final

If the current version of the project is "6.2.0", the new version set during the build will be "6.1.0.Final". A combination of properties may be used e.g.

    mvn install -DversionOverride=6.1.0.Final -DversionSuffix=rebuild-1

Using the above example, this would result in the version being "6.1.0.Final-rebuild-1".

### Snapshot Detection

The extension can detect snapshot versions and either preserve the snapshot or replace it with a real version. This is controlled by the property `versionSuffixSnapshot` (*Deprecated property `version.suffix.snapshot` for versions **3.8.1 and prior***). The default is false (i.e. remove SNAPSHOT and replace by the suffix).

    mvn install -DversionSuffixSnapshot=true

This means that the SNAPSHOT suffix will be kept.

### Suffix Stripping

Normally the tool will manipulate the version as given within the POM. However in certain scenarios it is desired that a known suffix is stripped from the version _before_ any further manipulators (e.g. REST, Version etc) are run. To activate this pass:

    -DversionSuffixStrip=

This will utilise the default suffix strip configuration (in regular expression form) of `(.*)(.jbossorg-\d+)$`. To configure this to be something different simply pass e.g.:

    -DversionSuffixStrip='(.*)(.MYSUFFIX)$'

<table bgcolor="#ffff00">
<tr>
<td>
    <b>NOTE</b> : From version 3.9 disabling is possible as follows:
</td>
</tr>
</table>

If the special keyword of `NONE` is used this will also disable the suffix (after it has been enabled) i.e.

    -DversionSuffixStrip= -DversionSuffixStrip=NONE


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

This is controlled by the property `versionOsgi` (*Deprecated property `version.osgi` for versions **3.8.1 and prior***). The default is true (i.e. make the versions OSGi compliant).

### Alternate Suffix Handling
It is possible to pass in a comma separated list of alternate suffixes via the property `versionSuffixAlternatives`. The default value is `redhat` which will be applied _if_ the current suffix does not match that. This is used during dependency alignment to validate strict alignment between differing suffix types (from the input REST or BOM data).
