---
title: "XML Manipulation"
---

### Overview

PME offers the ability to modify one or many xml files in the repository prior to running the build.

**This should not be used to modify XML POM files within the Maven Model but for e.g. assembly files**



### Configuration

The manipulator is controlled by the `xmlUpdate` property. The format is

    -DxmlUpdate=<file>:<xml-xpath-expression>:[<replacement-value>] [,....]

Multiple comma separated values may be supplied. If the replacement-value is **not** specified the operation becomes a _delete_ rather than an _update_.

The format for the _xpath-style_ expression is as used in [XPath](https://docs.oracle.com/javase/7/docs/api/javax/xml/xpath/XPath.html) and in the specification [here](https://www.w3.org/TR/xpath).

As an example:

    -DxmlUpdate='src/main/assembly/dep.xml://include[starts-with(.,'org.apache.tomcat')]:com.rebuild:servlet-api"'

This means:

* For the file _src/main/assembly/dep.xml_
  * Use the path expression _//include[starts-with(.,'org.apache.tomcat')]_ to locate the appropriate node.
  * Replace the value with _com.rebuild:servlet-api_.
