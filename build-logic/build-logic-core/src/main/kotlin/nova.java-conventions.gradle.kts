import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.common-conventions")
    id("nova.repository-conventions")
    `java-library`
}

val libs = providers.provider { the<LibrariesForLibs>() }

dependencies {
    testImplementation(platform(libs.flatMap { it.junit.bom }))
    testImplementation(libs.flatMap { it.junit.jupiter })
    testImplementation(libs.flatMap { it.junit.platformLauncher })
    testImplementation(libs.flatMap { it.kotlin.test.junit })
    testImplementation(libs.flatMap { it.slf4j.simple })
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
}