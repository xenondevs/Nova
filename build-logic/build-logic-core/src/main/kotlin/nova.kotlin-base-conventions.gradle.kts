import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    id("nova.java-conventions")
    kotlin("jvm")
}

val libs = providers.provider { the<LibrariesForLibs>() }

dependencies {
    testImplementation(libs.flatMap { it.mockk })
}

sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

kotlin {
    compilerOptions {
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        moduleName = project.name
        novaCompilerOptions(providers.gradleProperty("release").isPresent)
    }
}

tasks.named { it == "kotlinSourcesJar" }.configureEach { enabled = false }
