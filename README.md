# Overview

POM Manipulation Extension (PME) is a Maven tool to align the versions in your POMs according to some external reference, sort of like a BOM but much more extensive and without the added baggage of a BOM declaration.

It is suppplied as a core library, a Maven extension (in the sense of installing to `lib/ext`, not `pom.xml` `<extensions/>`) and a command line tool.

This extension combines many of the features of [VMan](https://github.com/jdcasey/pom-version-manipulator), [Maven Versioning Extension](https://github.com/jdcasey/maven-versioning-extension) and [Maven Dependency Management Extension](https://github.com/jboss/maven-dependency-management-extension).

For more details on usage see the documentation [here](https://release-engineering.github.io/pom-manipulation-ext).

## Developing

### Prequisites

* Java 1.6 or later
* Maven 3.0.4 or later.

### Commits

An example `codestyle.xml` is supplied which is compatible with Eclipse and may also be imported into IntelliJ. There is also an IntelliJ compatible copyright template suitable for use when the project is imported into IntelliJ.

### Building

`mvn clean install` will compile and run all of the unit tests. In order to run the integration tests `-Prun-its` should be passed in. For the command line tests, it is possible to run a specific one by passing e.g. `-Dtest=DefaultCliIntegrationTest -Dtest-cli=<test name e.g. depmgmt-strict-mode-exact>`.

In order to edit the website at https://release-engineering.github.io/pom-manipulation-ext checkout the `gh-pages` branch. It is possible to use Jekyll (https://help.github.com/articles/using-jekyll-with-pages) to preview the changes. Jekyll can be run with `jekyll serve --watch -V`
