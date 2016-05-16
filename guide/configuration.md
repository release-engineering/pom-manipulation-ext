---
title: "Configuration Files"
---

### Overview

Normally all configuration is passed in via command line properties e.g.

    -Dconfiguration-key=value

However, PME also offers the ability to read a local configuration file. This file may be in either YAML, JSON or Java Properties format.

* Only one configuration file must exist.
* The file must be named either `pme.yaml`, `pnc.yaml`, `pnc.json` or `pme.properties`.
* The file must be placed at the execution root (i.e. the root directory of the SCM checkout).

**Note**: The configuration will _override_ any command line properties.

### Yaml

The format is as follows

    pme:
        key : value
        key2 : value2
    other-configuration:
        ....


Any other configuration sections are ignored by PME.

### JSON

The format is as follows

    {
        pme: {
            key : value,
            key2 : value2
        }
        other-configuration:
            ....
    }

Any other configuration sections are ignored by PME.

### Properties

The file should be in a standard [java properties file format](https://docs.oracle.com/javase/tutorial/essential/environment/properties.html).
