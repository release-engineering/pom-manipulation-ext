

[![Build Status (Travis CI)](https://travis-ci.org/release-engineering/pom-manipulation-ext.svg?branch=master)](https://travis-ci.org/release-engineering/pom-manipulation-ext.svg?branch=master)


## Introduction

POM Manipulation Extension (PME) is a Maven tool to align the versions in your POMs according to some external reference, sort of like a BOM but much more extensive and without the added baggage of a BOM declaration.

It is supplied as a core library, a Maven extension (in the sense of installing to `lib/ext`, not [pom extensions](https://maven.apache.org/pom.html#Extensions)) and a command line tool.

This extension combines many of the features of [VMan](https://github.com/jdcasey/pom-version-manipulator), [Maven Versioning Extension](https://github.com/jdcasey/maven-versioning-extension) and [Maven Dependency Management Extension](https://github.com/jboss/maven-dependency-management-extension).

## Documentation

For details on usage see the documentation [here](https://release-engineering.github.io/pom-manipulation-ext).

## Developing

Contributions are welcome! To contribute sample Groovy scripts (for this project or the sibling
[Gradle Manipulator](https://github.com/project-ncl/gradle-manipulator) project) please see the
[Groovy Examples](https://github.com/project-ncl/manipulator-groovy-examples) project.

### Prerequisites

* Java 1.8 or later
* Maven 3.1 or later.

### Copyright

The `.idea` folder contains a copyright template suitable for use when the project is imported into IntelliJ.

### Building

`mvn clean install` will compile and run all of the unit tests. In order to run the integration tests `-Prun-its` should be passed in. For the command line tests, it is possible to run a specific one by passing e.g. `-Dtest=DefaultCliIntegrationTest -Dtest-cli=<test name e.g. depmgmt-strict-mode-exact>`.

The system is setup via a `.travis.yml` to build all pull requests in Travis. Further, it will build master branch and utilise the `.travis.settings.xml` to deploy to the Sonatype snapshot repository from Travis.

In order to edit the website at https://release-engineering.github.io/pom-manipulation-ext checkout the `gh-pages` branch.
It is possible to use Jekyll (https://help.github.com/articles/using-jekyll-with-pages) to preview the changes.
Jekyll can be run with `jekyll serve --watch -V` and may be installed in Fedora via the `rubygem-jekyll` package.

### IntelliJ

The following plugin is required:

 * https://plugins.jetbrains.com/plugin/6317-lombok/

It is also recommended to install:

 * https://plugins.jetbrains.com/plugin/6546-eclipse-code-formatter
    * (which allows importing of the code style)
 * https://plugins.jetbrains.com/plugin/7442-gmavenplus-intellij-plugin
    * (especially if using the Groovy [example project](https://github.com/project-ncl/manipulator-groovy-examples) )


## Coding


### Style

Eclipse compatible `codestyle.xml` and `eclipse.importorder` files are supplied inside the `ide-config` directory which
may also be imported into IntelliJ via the EclipseCodeFormatter. There is also an IntelliJ compatible copyright template
suitable for use when the project is imported into IntelliJ.

### Exception Handling

The three built in Exceptions all support SLF4J style `{}` substitution parameters

    ManipulationException
    RestException
    ManipulationUncheckedException

For example

    throw new ManipulationException( "Unable to detect charset for file {}", jsonFile, exceptionObject );
    throw new ManipulationException( "Internal project dependency resolution failure ; replaced {} by {}", old, d );
    throw new ManipulationException( "Unable to parse groovyScripts", exceptionObject );


## Testing

The tool has both unit tests and integration tests. The integration tests will only be run if the specified profile has been
activated e.g.

    mvn clean install -Prun-its

The unit tests make use of the `system-rules` library e.g.

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog().muteForSuccessfulTests();

    @Rule
    public final TestRule restoreSystemProperties = new RestoreSystemProperties();

See https://stefanbirkner.github.io/system-rules for further information on this.

A subset of the integration tests may be run by using the following example commands:

    mvn -Dmaven.javadoc.skip=true -Denforcer.skip=true -DfailIfNoTests=false -Prun-its clean install
        -Dtest=CircularIntegrationTest -Dtest-cli=circular-dependencies-test-second

    mvn -Dmaven.javadoc.skip=true -DfailIfNoTests=false -Prun-its clean install
        -Dtest=DefaultCliIntegrationTest -Dtest-cli=property-clash

The main test is the `DefaultCliIntegrationTest`.
