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

- **asm-analyzer** - A basic simulation of a method to calculate data-flow and control-flow of a given method's
execution. The actual value is not calculated but rather just value types pushed and popped from stack and from the
LVT.

- **asm-executor** - TODO - Create a method execution simulation to figure out what values are pushed
and popped from the JVM stack at each instruction.

- **asm-remapper** - TODO - Rename a loaded class pool with provided name mappings from an external source.

- **asm-kotlin** - TODO - A replica of *asm-core* with raw kotlin class support.

## Install
Add the desired module as a gradle dependency to your gradle build file.

**Groovy**
```groovy
dependencies {
    // asm-core
    implementation 'org.spectral.asm:asm-core:1.0.0'

    // asm-analyzer
    implementation 'org.spectral.asm:asm-analyzer:1.0.0'

    // asm-executor
    implementation 'org.spectral.asm:asm-executor:1.0.0'

    // asm-remapper
    implementation 'org.spectral.asm:asm-remapper:1.0.0'

    // asm-kotlin
    implementation 'org.spectral.asm:asm-kotlin:1.0.0'
}
```

**Kotlin DSL**
```kotlin
dependencies {
    // asm-core
    implementation("org.spectral.asm:asm-core:1.0.0")

    // asm-analyzer
    implementation("org.spectral.asm:asm-analyzer:1.0.0")

    // asm-executor
    implementation("org.spectral.asm:asm-executor:1.0.0")

    // asm-remapper
    implementation("org.spectral.asm:asm-remapper:1.0.0")

    // asm-kotlin
    implementation("org.spectral.asm:asm-kotlin:1.0.0")
}
```

## Usage

Below are some basic examples on how to use each library component. For documentation and more usage and examples
please see the project Wiki pages.

#### Core
The following is an example on how to load classes from a Jar and rewrite them after modification
to a new Jar files.

```kotlin
/*
 * Create an empty class pool.
 */
val pool = ClassPool()

// Input Jar file
val inputJar = File("/path/to/my/input.jar")

/*
 * Load all class bytes from the input
 * Jar file into a hash set.
 */
val classBytes = JarUtil.readJar(inputJar)

/*
 * Add each class byte array read from the jar file
 * as a class into the class pool.
 */
classBytes.forEach { bytes ->
    pool.addClass(bytes)
}

/*
 * Verify our classes where added.
 */
println("Loaded Classes: ${pool.size}")

/*
 * Do some modifications. Here we just print each
 * class name in the pool but you can modify any variables on the
 * class object.
 */
pool.classes.forEach { cls ->
    println("Class Name: ${cls.name} compiled with JVM version: ${cls.version}")
}
```

**To write the pool to a Jar file**
```kotlin
/*
 * Save the class pool to a new output Jar file.
 */
val outputJar = File("/path/to/my/output.jar")

JarUtil.writeJar(outputJar, pool)
```

#### Analyzer
Below is a simple example on how you can fetch a **data-flow** model of a method's simulation using
the Spectral ASM analyzer module.

```kotlin
/*
 * Create a class pool and load classes from a Jar file
 * into the class pool.
 */
val pool = ClassPool()
val inputJar = File("/path/to/my/input.jar")

JarUtil.readJar(inputJar).forEach { pool.addClass(it) }

/*
 * Get an instance of a method from the class pool.
 */
val myClass = pool["myClass"]!!
val myMethod = myClass.findMethod("myMethod", "()V")!!

/*
 * Run the method analyzer on [myMethod] instance.
 */
val results = MethodAnalyzer.analyze(myMethod)

/*
 * Explore the resulting frames. These are a basic analysis of the data-flow during
 * "myMethod's" execution on the JVM. No absolute values are calculated. Just where the values
 * pushed and popped from the stack origin instruction that created them.
 */
results.frames.forEach { insn, values ->
    println("=====================")
    println("Instruction: ${insn}")
    values.forEach { value ->
        println("\tValue: $value :: Origin Insn: ${value.parents.first().opcode}")
    }
}
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