# Maven Versioning Extension

A Maven extension which manages the current version of the current project.

## Automatic version increment

The extension can be used to append a version suffix/qualifier
to the current project, and then apply an incremented index to the version
to provide a unique release version.  For example, if the current
project version is 1.0.0.GA, the extension can automatically set the version
to 1.0.0.GA-rebuild-1, 1.0.0.GA-rebuild-2, etc.

The extension is configured using the system property **version.incremental.suffix**.

    mvn install -Dversion.incremental.suffix=rebuild 

The Maven repository metadata will be checked to locate the latest released version of the project artifacts, and the next version is selected by the extension.

## Manual version suffix

The version suffix to be appended to the current project can be manually selected using the property **version.suffix**

    mvn install -Dversion.suffix=release-1

If the current version of the project is "1.2.0.GA", the new version set during the build will be "1.2.0.GA-release-1".


