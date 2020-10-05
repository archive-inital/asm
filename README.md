# Spectral ASM Library
This library contains a simple to use ASM tree extension which loads and holds
more information than the native ASM tree library. This is in use by the Spectral
powered projects and is available as an independent gradle dependency.

## Overview
This project contains multiple utilities as independent gradle modules. Each module
can be used separately. Below is an overview of the modules and a short description of 
what they offer.

- **asm-core** - The core model used to load components of Java classes from either individual .class files
or from Jar files.

- **asm-analyzer** - TODO - A basic simulation of a method to calculate data-flow and control-flow of a given method's
execution. The actual value is not calculated but rather just value types pushed and popped from stack and from the
LVT.

- **asm-executor** - TODO - Create a method execution simulation to figure out what values are pushed
and popped from the JVM stack at each instruction.

- **asm-remapper** - TODO - Rename a loaded class pool with provided name mappings from an external source.

- **asm-kotlin** - TODO - A replica of *asm-core* with raw kotlin class support.

## Install
Add the spectral maven repository to your Gradle build file.

```groovy
repositories {
    maven { url 'https://repo.spectralclient.org/repository/spectral/' }
}
```

**NOTE** This repository is only a maven proxy repo to the GitHub packages maven repository. Currently, GitHub
requires an ACCESS_TOKEN to download any packages from it's maven package repository as it's not designed to be a central repository.
To get around this the repo above takes care of all GitHub authentication on the backend.

Add the desired module as a gradle dependency to your gradle build file.

**Groovy**
```groovy
dependencies {
    // asm-core
    implementation 'org.spectral:asm-core:1.0.0'

    // asm-analyzer
    implementation 'org.spectral:asm-analyzer:1.0.0'

    // asm-executor
    implementation 'org.spectral:asm-executor:1.0.0'

    // asm-remapper
    implementation 'org.spectral:asm-remapper:1.0.0'

    // asm-kotlin
    implementation 'org.spectral:asm-kotlin:1.0.0'
}
```

## Usage

Below are some basic examples on how to use each library component. For documentation and more usage and examples
please see the project Wiki pages.

#### Core
The following is an example on how to load classes from a Jar and rewrite them after modification
to a new Jar files.

```kotlin
// Input Jar file
val inputJar = File("/path/to/my/input.jar")

// Output Jar file
val outputJar = File("/path/to/my/output.jar")

/*
 * Create a class pool from the inputJar classes.
 */
val pool = ClassPool.loadFromJar(inputJar)

/*
 * Loop through all the classes.
 */
pool.classes.forEach { classNode ->
    /*
     * We could make changes if we want to the classes.
     */
    println("Class Name: ${classNode.name}")
}

/*
 * Save the classes in the pool to the outputJar file.
 */
pool.saveToJar(outputJar)
```

## Contributing
Contributions are always welcome. Make sure to follow the guidelines for pull-requests and please make
all pull-requests targeting the 'develop' branch, or the appropriate feature branch if one exists.

[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/0)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/0)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/1)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/1)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/2)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/2)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/3)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/3)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/4)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/4)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/5)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/5)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/6)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/6)
[![](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/images/7)](https://sourcerer.io/fame/kyle-escobar/spectral-powered/asm/links/7)