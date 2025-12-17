# Iridium

[![](https://git.karmakrafts.dev/kk/iridium/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/iridium/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.maven.apache.org%2Fmaven2%2Fdev%2Fkarmakrafts%2Firidium%2Firidium%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/iridium/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Firidium%2Firidium%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/iridium/-/packages)
[![](https://img.shields.io/badge/2.3.0-blue?logo=kotlin&label=kotlin)](https://kotlinlang.org/)
[![](https://img.shields.io/badge/documentation-black?logo=kotlin)](https://docs.karmakrafts.dev/iridium)

Iridium is an in-process compiler testing framework for Kotlin using the Kotlin embeddable compiler and Kotlin Test.  
It allows testing compiler behaviour and FIR/IR compiler plugins.  

It currently has the following main features:

* Support for Kotlin/JVM, Kotlin/JS and Kotlin/Native
* Fully integrated test compiler pipeline using the new `Fir2IrPipeline` with DSL
* Assertion matchers for compiler reports with DSL
* Assertion matchers for Kotlin IR with DSL
* Assertion matchers for Kotlin FIR with DSL
* Colored Kotlin IR highlighting in assertion errors and available as test API
* Automatic scope tracking to add more context to assertion errors
* Add dependencies by mentioning a library type once

### How to use it

First, add the official Maven Central repository to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots")
        mavenCentral()
    }
}
```

Then add a dependency on the library in your root buildscript:

```kotlin
dependencies {
    testImplementation("dev.karmakrafts.iridium:iridium:<version>")
}
```

### Test DSL for reports, FIR and IR

```kotlin
@Test
fun `My compiler IR test`() = runCompilerTest {
    pipeline {
        defaultPipelineSpec()
    }
    source("""
        @Suppress("UNCHECKED_CAST")
        fun <T> test(value: T): T = value
        fun main(args: Array<String>) {
            println("Hello, World")
        }
    """.trimIndent()) // *1
    compiler shouldNotReport { error() }
    result irMatches {
        getChild<IrFunction> { it.name.asString() == "main" }.matches("main") {
            returns { unit() }
            hasValueParameter("args") { type(types.stringType.array()) }
        }
        getChild<IrFunction> { it.name.asString() == "test" }.matches("test") {
            hasAnnotation(type("kotlin/Suppress"))
            hasTypeParameter("T")
            returns { typeParameter("T") }
            hasValueParameter("value") { typeParameter("T") }
        }
        containsChild<IrCall> { it.target.name.asString() == "println" }
    }
}
```
**\*1: The source code assigned in the multiline string will have highlighting in supported IDEs.**