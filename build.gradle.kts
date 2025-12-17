/*
 * Copyright 2025 Karma Krafts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
}

tasks {
    val sourcesJar by getting {
        dependsOn(compileJava)
    }
    val javadocJar by named<Jar>("javadocJar") {
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
            from(zipTree(javadocJar.outputs.files.first()))
            into(docsDir)
        }
    }
}

dokka {
    moduleName = project.name
    pluginsConfiguration {
        html {
            footerMessage = "&copy; ${ZonedDateTime.now().year} Karma Krafts"
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
    setRepository("github.com", "karmakrafts/iridium")
    apache2License()
    with(GitLabCI) {
        karmaKraftsDefaults()
    }
}

nexusPublishing {
    authenticatedSonatype()
}