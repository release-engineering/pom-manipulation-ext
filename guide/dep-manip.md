---
title: "Dependency Manipulation"
---

### Overview

PME can override a set of dependency versions using a remote pom (BOM) file. By default, all dependencies listed in the remote pom will be added to the current build. This has the effect of overriding matching transitive dependencies, as well as those specified directly in the pom. 

Use the `dependencyManagement` property to list your BOMs:

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0

Multiple remote dependency-management poms can be specified using a comma separated list of GAVs (groupId, artifactId, version). The poms are specified in order of priority, so if the remote boms contain some of the same dependencies, the versions listed in the first bom in the list will be used.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0,org.bar:my-dep-pom:2.0


**NOTE:** For existing dependencies that reference a property, PME will update this property with the new version. If the property can't be found (e.g. it was inherited), a new one will be injected at the top level. This update of the property's value **may** implicitly align other dependencies using the same property that were not explicitly requested to be aligned.

#### Parent Version Override

PME will also change any parent reference it finds that matches an entry in the remote BOM.

For example:

      <parent>
         <groupId>org.switchyard</groupId>
         <artifactId>switchyard-parent</artifactId>
         <version>2.0.0.Alpha1</version>

will change to:

      <parent>
         <groupId>org.switchyard</groupId>
         <artifactId>switchyard-parent</artifactId>
         <version>2.0.0.Alpha1-rebuild-1</version>


### Direct Dependencies

By default the extension will override dependencies using declarations from the remote BOM. However, by setting the property `overrideDependencies` to `false`, the behavior can be disabled:

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0 -DoverrideDependencies=false

Note that this will still alter any external parent references.

### Direct/Transitive Dependencies

By default the extension will inject all dependencies declared in the remote BOM. This will also override dependencies that are not directly specified in the project. If these transitive dependencies should not be overridden, the option `overrideTransitive` can be set to `false` to disable this feature.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0 -DoverrideTransitive=false

### Exclusions and Per Module Overrides

In a multi-module build it is considered good practice to coordinate dependency version among the modules using dependency management.  In other words, if module A and B both use dependency X, both modules should use the same version of dependency X.  Therefore, the default behaviour of this extension is to use a single set of dependency versions applied to all modules.

#### Per-Module Override

However, there are certain cases where it is useful to use different versions of the same dependency in different modules.  For example, if the project includes integration code for multiple versions of a particular API. In that case it is possible to apply a version override to a specific module of a multi-module build using a property starting with `dependencyExclusion.` and having the following format:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=[version]

For example to apply an explicit dependency override only to module B of project foo.

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=4.10


#### Per-Module Prevention of Override

It is also possible to **prevent overriding dependency versions** on a per module basis:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=

For example:

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=

#### Override Prevention with Wildcards

Likewise, you can prevent overriding a dependency version across the entire project using a wildcard:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@*=

For example:

    mvn install -DdependencyExclusion.junit:junit@*=

Or, you can prevent overriding a dependency version across the entire project where the groupId matches, using multiple wildcards:

    mvn install -DdependencyExclusion.[groupId]:*@*=

For example:

    mvn install -DdependencyExclusion.junit:*@*=


### Strict Mode Version Alignment

When aligning dependency versions to some shared standard, it's possible to introduce incompatibilities that stem from too large of a version change. For instance, while it might be safe to revise a dependency's version from 1.5 to 1.5.1, it may not be safe to revise it to 2.0, or even 1.6.

In cases where this is a concern, and for dependencies whose versions are aligned via a BOM (not via explicit overrides, as explained above), strict-mode version alignment checks can be enabled using:

    mvn install -DstrictAlignment=true

This will detect cases where the adjusted version doesn't start with the old version (i.e. 1.0 -> 1.0.1), **not** do the alignment and report the mismatch as a warning in the build's console output.

If, instead, the build should fail when strict-mode checks are violated, add the `strictViolationFails=true` property:

    mvn install -DstrictAlignment=true -DstrictViolationFails=true

This will cause the build to fail with a ManipulationException, and prevent the extension from rewriting any POM files.

**NOTE:** dependency exclusions they will not work if the dependency uses a version property that has been changed by another dependency modification. Explicit version override will overwrite the property value though.

### Dependency Property Injection (DEPRECATED)

The extension will automatically set properties which match the version overrides.  These properties can be used, for example, in resource filtering in the build.  By default the extension supports two different formats for the properties. It is controlled by the property:

    -DversionPropertyFormat=[VG|VGA|NONE]

Where:

* `VG` is `version.<group>` (e.g. `version.org.slf4j`)
* `VGA` is `version.<group>.<artifact>`
* `NONE` disables the injection

The default is `NONE`.


