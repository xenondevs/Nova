import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("nova.java-conventions")
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        
        optIn.addAll(
            "kotlin.io.path.ExperimentalPathApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalTypeInference"
        )
        
        freeCompilerArgs.addAll(
            "-Xjvm-default=all", // Emit JVM default methods for interface declarations with bodies
        )
        
        if (!project.hasProperty("release")) {
            freeCompilerArgs.addAll(
                "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
            )
        }
    }
}

tasks.matching { it.name == "kotlinSourcesJar" }.configureEach { enabled = false }