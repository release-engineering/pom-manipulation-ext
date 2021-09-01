
![Build status (GitHub Actions)](https://github.com/release-engineering/pom-manipulation-ext/workflows/CI/badge.svg)


# Table of Contents

<!-- TocDown Begin -->
* [Introduction](#introduction)
* [Documentation](#documentation)
* [Developing](#developing)
<!-- TocDown End -->


# Introduction

POM Manipulation Extension (PME) is a Maven tool to align the versions in your POMs according to some external reference, sort of like a BOM but much more extensive and without the added baggage of a BOM declaration.

It is supplied as a core library, a Maven extension (in the sense of installing to `lib/ext`, not [pom extensions](https://maven.apache.org/pom.html#Extensions)) and a command line tool.

This extension combines many of the features of [VMan](https://github.com/jdcasey/pom-version-manipulator), [Maven Versioning Extension](https://github.com/jdcasey/maven-versioning-extension) and [Maven Dependency Management Extension](https://github.com/jboss/maven-dependency-management-extension).

# Documentation

For details on usage see the documentation [here](https://release-engineering.github.io/pom-manipulation-ext).

# Developing

Contributions are welcome! To contribute sample Groovy scripts (for this project or the sibling
[Gradle Manipulator](https://github.com/project-ncl/gradle-manipulator) project) please see the
[Groovy Examples](https://github.com/project-ncl/manipulator-groovy-examples) project.

Please see [the developer guide](DEVELOPING.md) for further instructions.
