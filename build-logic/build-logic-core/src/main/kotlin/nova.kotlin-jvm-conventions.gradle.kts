import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.java-conventions")
    id("nova.kotlin-compiler-conventions")
    kotlin("jvm")
}

val libs = providers.provider { the<LibrariesForLibs>() }

dependencies {
    testImplementation(libs.flatMap { it.mockk })
}

sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

kotlin {
    compilerOptions.moduleName = project.name
}

tasks.named { it == "kotlinSourcesJar" }.configureEach { enabled = false }
