---
title: "Dependency Manipulation"
---

### Overview

PME can override a set of dependency versions using a remote source which may be either a pom (BOM) file or a remote REST endpoint.

#### Remote POM

By default, all dependencies listed in the remote pom will be added to the current build. This has the effect of overriding matching transitive dependencies, as well as those specified directly in the pom.

Use the `dependencyManagement` property to list your BOMs:

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0

Multiple remote dependency-management poms can be specified using a comma separated list of GAVs (groupId, artifactId, version). The poms are specified in order of priority, so if the remote boms contain some of the same dependencies, the versions listed in the first bom in the list will be used.

    mvn install -DdependencyManagement=org.foo:my-dep-pom:1.0,org.bar:my-dep-pom:2.0


#### REST Endpoint

Alternatively, rather than using a remote BOM file as a source, it is possible to instruct PME to prescan the project, collect up all group:artifact:version's used and call a REST endpoint using the property `-DrestURL` (provided by https://github.com/project-ncl/dependency-analysis) which will then return a list of possible new versions. By default it will pass *all* GAVs to the endpoint, however, to avoid problems with large requests, if the endpoint returns an error the batch is automatically split into smaller chunks in an attempt to reduce load on the endpoint.

By default PME will use the 'current' REST API. It can be configured to use the deprecated previous API with the flag `-DrestProtocol=deprecated`.

The REST endpoint should follow:

<table>
<tr>
   <th id="Parameters">Parameters</th>
   <th id="Returns">Returns</th>
</tr>
<tr>
<td>
   <pre lang="xml" style="font-size: 10px">
[
    {
        "groupId": "org.foo",
        "artifactId": "bar",
        "version": "1.0.0.Final"
    },
    ...
]
    </pre>
</td>
<td>
  <pre lang="xml" style="font-size: 10px">
[
    {
        "groupId": "org.foo",
        "artifactId": "bar",
        "version": "1.0.0.Final",
        "availableVersions": ["1.0.0.Final-rebuild-2",
"1.0.0.Final-rebuild-1", "1.0.1.Final-rebuild-1"],
        "bestMatchVersion": "1.0.0.Final-rebuild-2",
        "blacklisted": false,
        "whitelisted": true
    },
    ...
]  </pre>
</td>
</tr>
</table>


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

### Exclusions and Overrides

In a multi-module build it is considered good practice to coordinate dependency version among the modules using dependency management.  In other words, if module A and B both use dependency X, both modules should use the same version of dependency X.  Therefore, the default behaviour of this extension is to use a single set of dependency versions applied to all modules.

It is possible to flexibly override or exclude a dependency globally or on a per module basis. The property starts with `dependencyExclusion.` and has the following format:

    mvn install -DdependencyExclusion.[groupId]:[artifactId]@[moduleGroupId]:[moduleArtifactId]=[version] | ,+[group:artifact]...

**NOTE:** `dependencyOverride` is an alias for `dependencyExclusion` and functions _exactly the same_. If both are set then they will be merged and an error thrown if they clash.


#### Global Version Override

Sometimes it is more convenient to use the command line rather than a BOM. Therefore extending the above it is possible to set the version of a dependency via:

    mvn install -DdependencyExclusion.junit:junit@*=4.10-rebuild-10

This will, throughout the entire project (due to the wildcard), apply the explicit 4.10-rebuild-10 version to the junit:junit dependency.

**NOTE:** Explicit overrides like this will take precedence over strict alignment and the BOM.


#### Per-Module Version Override

However, there are certain cases where it is useful to use different versions of the same dependency in different modules.  For example, if the project includes integration code for multiple versions of a particular API. In that case it is possible to apply a version override to a specific module of a multi-module build. For example to apply an explicit dependency override only to module B of project foo.

    mvn install -DdependencyExclusion.junit:junit@org.foo:moduleB=4.10

**NOTE:** Explicit overrides like this will take precedence over strict alignment and the BOM.


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

#### Dependency Exclusion Addition

It is also possible to inject specific exclusions into a dependency. For instance

    mvn install -DdependencyExclusion.junit:junit@*=4.10-rebuild-1,+slf4j:slf4j

will, as per above, apply the explicit 4.10-rebuild-10 version to the junit:junit dependency but also add an exclusion e.g.

    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.1</version>
      <exclusions>
        <exclusion>
          <artifactId>slf4j</artifactId>
          <groupId>slf4j</groupId>
        </exclusion>
      </exclusions>
    </dependency>

Multiple exclusions may be added via comma separators e.g.

    mvn install -DdependencyExclusion.junit:junit@*=+slf4j:slf4j,+commons-lang:commons-lang


### Strict Mode Version Alignment

When aligning dependency versions to some shared standard, it's possible to introduce incompatibilities that stem from changing the version. This may be due to unexpected API changes. While _in general_ it might be safe to revise a dependency's version from 1.5 to 1.5.1, it may not be safe to revise it to 2.0, or even 1.6.

In cases where this is a concern, and for dependencies whose versions are aligned via a BOM (not via explicit overrides, as explained above), strict-mode version alignment checks can be enabled using:

    mvn install -DstrictAlignment=true

This will detect cases where the adjusted version, once OSGi and suffix are handled, does not match with the old version, **not** do the alignment and report the mismatch as a warning in the build's console output. For example, if the incremental or manual suffix is configured to be `rebuild` then these are valid changes.

| Original Version | Potential New Versions |
| --- | --- |
| 2.6 | 2.6-rebuild-2 ; 2.6.0.rebuild-4 |
| 3 | 3.0.0.rebuild-1 ; 3-rebuild-1 |
| 3.1 | 3.1.0.rebuild-1 ; 3.1-rebuild-1 |

Note that it will not consider 3 -> 3.1 as a valid transition.

If, instead, the build should fail when strict-mode checks are violated, add the `strictViolationFails=true` property:

    mvn install -DstrictAlignment=true -DstrictViolationFails=true

This will cause the build to fail with a ManipulationException, and prevent the extension from rewriting any POM files.

**NOTE:** dependency exclusions they will not work if the dependency uses a version property that has been changed by another dependency modification. Explicit version override will overwrite the property value though.


If the property `strictAlignmentIgnoreSuffix` is set to true then the comparison will ignore the suffix depicted by `version.incremental.suffix` or `version.suffix` during version comparisons. It will also only allow alignment to a higher incrementing suffix e.g.

    3.1.0.Final-rebuild-1 --> 3.1.0.Final-rebuild-3


### Dependency Relocations

In order to handle the situation where one GAV is changed to another (e.g. from community to product) the relocation manipulator can be used. Multiple relocations may be comma separated and an optional version component may be added; the version will override any prior version used in the dependency. Note this is akin to using the dependencyExclusion functionality with an explicit version. The artifact override is optional.

    -DdependencyRelocations.oldGroupId:[oldArtifact]@newGroupId:[newArtifactId]=[version],...

### Dependency Removal

If the property `-DdependencyRemoval=group:artifact,....` is set, PME will remove the specified dependency from the POM files. The argument should be a comma separate list of group:artifact.
