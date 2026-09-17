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
        moduleName = project.name
        
        optIn.addAll(
            "kotlin.io.path.ExperimentalPathApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalTypeInference",
            "kotlin.contracts.ExperimentalContracts"
        )
        
        freeCompilerArgs.addAll(
            "-Xname-based-destructuring=complete",
            "-Xcollection-literals"
        )
        
        if (!providers.gradleProperty("release").isPresent) {
            freeCompilerArgs.addAll(
                "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
            )
        }
    }
}

tasks.named { it == "kotlinSourcesJar" }.configureEach { enabled = false }
