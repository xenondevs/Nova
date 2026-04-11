import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    id("nova.java-conventions")
    kotlin("jvm")
}

val libs = the<LibrariesForLibs>()

dependencies {
    testImplementation(libs.mockk)
}

sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

kotlin {
    compilerOptions {
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        
        optIn.addAll(
            "kotlin.io.path.ExperimentalPathApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalTypeInference",
            "kotlin.contracts.ExperimentalContracts"
        )
        
        freeCompilerArgs.addAll(
            "-Xcontext-parameters"
        )
        
        if (!project.hasProperty("release")) {
            freeCompilerArgs.addAll(
                "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
            )
        }
    }
}

tasks.matching { it.name == "kotlinSourcesJar" }.configureEach { enabled = false }