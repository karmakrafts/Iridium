import dev.karmakrafts.conventions.GitLabCI
import dev.karmakrafts.conventions.apache2License
import dev.karmakrafts.conventions.authenticatedSonatype
import dev.karmakrafts.conventions.configureJava
import dev.karmakrafts.conventions.defaultDependencyLocking
import dev.karmakrafts.conventions.setProjectInfo
import dev.karmakrafts.conventions.setRepository
import dev.karmakrafts.conventions.signPublications
import java.time.ZonedDateTime

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    signing
    `maven-publish`
    alias(libs.plugins.dokka)
    alias(libs.plugins.karmaConventions)
    alias(libs.plugins.gradleNexus)
}

group = "dev.karmakrafts.iridium"
version = GitLabCI.getDefaultVersion(libs.versions.iridium)
configureJava(rootProject.libs.versions.java)
if (GitLabCI.isCI) defaultDependencyLocking()

java {
    withSourcesJar()
    withJavadocJar()
}

val frameworkConfiguration: Configuration by configurations.creating

configurations {
    compileOnly { extendsFrom(frameworkConfiguration) }
    testApi { extendsFrom(frameworkConfiguration) }
}

dependencies {
    frameworkConfiguration(libs.junit.api)
    frameworkConfiguration(libs.kotlin.compiler.embeddable)
    frameworkConfiguration(libs.kotlin.stdlib)
    frameworkConfiguration(libs.kotlin.reflect)
    frameworkConfiguration(libs.kotlin.test)
    frameworkConfiguration(libs.annotations)
}

tasks {
    val sourcesJar by getting {
        dependsOn(compileJava)
    }
    named<Jar>("javadocJar") {
        dependsOn(dokkaGeneratePublicationHtml)
        from(dokkaGeneratePublicationHtml)
    }
    test {
        useJUnitPlatform()
    }
}

dokka {
    moduleName = project.name
    pluginsConfiguration {
        html {
            footerMessage = "(c) ${ZonedDateTime.now().year} Karma Krafts & associates"
        }
    }
}

signing {
    signPublications()
}

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
    setProjectInfo(rootProject.name, "Testing framework for Kotlin compiler plugins using a custom compiler driver.")
    setRepository("gitlab.com/karmakrafts/iridium")
    apache2License()
    with(GitLabCI) {
        karmaKraftsDefaults()
    }
}

nexusPublishing {
    authenticatedSonatype()
}