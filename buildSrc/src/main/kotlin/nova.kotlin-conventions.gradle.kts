import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("nova.java-conventions")
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        
        optIn.addAll(
            "kotlin.io.path.ExperimentalPathApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalTypeInference"
        )
        
        if (!project.hasProperty("release")) {
            freeCompilerArgs.addAll(
                "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
            )
        }
    }
}

tasks.matching { it.name == "kotlinSourcesJar" }.configureEach { enabled = false }