import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.common-conventions")
    `java-library`
}

val libs = the<LibrariesForLibs>()

repositories {
    mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") }}
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platformLauncher)
    testImplementation(libs.kotlin.test.junit)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    test {
        useJUnitPlatform()
        maxParallelForks = Runtime.getRuntime().availableProcessors()
    }
}