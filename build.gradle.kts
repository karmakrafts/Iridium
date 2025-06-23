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

dependencies {
    api(libs.junit.api)
    api(libs.kotlin.compiler.embeddable)
    api(libs.kotlin.native.compiler.embeddable)
    api(libs.kotlin.stdlib)
    api(libs.kotlin.reflect)
    api(libs.kotlin.test)
    api(libs.annotations)
    implementation(libs.oshi.core)
    implementation(libs.jansi)
}

tasks {
    val sourcesJar by getting {
        dependsOn(compileJava)
    }
    val javadocJar = named<Jar>("javadocJar") {
        dependsOn(dokkaGeneratePublicationHtml)
        from(dokkaGeneratePublicationHtml)
    }
    test {
        useJUnitPlatform()
    }
    System.getProperty("publishDocs.root")?.let { docsDir ->
        register("publishDocs", Copy::class) {
            dependsOn(javadocJar)
            mustRunAfter(javadocJar)
            from(zipTree(javadocJar.get().outputs.files.first()))
            into(docsDir)
        }
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
    setRepository("github.com/karmakrafts/iridium")
    apache2License()
    with(GitLabCI) {
        karmaKraftsDefaults()
    }
}

nexusPublishing {
    authenticatedSonatype()
}