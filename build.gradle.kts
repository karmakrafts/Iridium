import dev.karmakrafts.conventions.GitLabCI
import dev.karmakrafts.conventions.apache2License
import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.defaultDependencyLocking
import dev.karmakrafts.conventions.setProjectInfo
import dev.karmakrafts.conventions.setRepository
import dev.karmakrafts.conventions.signPublications

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    signing
    `maven-publish`
    alias(libs.plugins.karmaConventions)
    alias(libs.plugins.gradleNexus)
}

group = "dev.karmakrafts.iridium"
version = GitLabCI.getDefaultVersion(libs.versions.iridium)
configureJava(rootProject.libs.versions.java)
if (GitLabCI.isCI) defaultDependencyLocking()

dependencies {
    api(libs.junit.api)
    api(libs.kotlin.compiler.embeddable)
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    api(libs.kotlin.test)
}

tasks {
    test {
        useJUnitPlatform()
    }
}

publishing {
    setProjectInfo(rootProject.name, "Testing framework for Kotlin compiler plugins using a custom compiler driver.")
    setRepository("gitlab.com/karmakrafts/iridium")
    apache2License()
    with(GitLabCI) { karmaKraftsDefaults() }
}

signing {
    signPublications()
}