
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("org.jetbrains.dokka") version libs.versions.dokka
    id("xyz.xenondevs.jar-loader-gradle-plugin")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    implementation(project(":nova-loader"))
}

subprojects {
    group = "xyz.xenondevs.nova"
    version = properties["version"] as String
    
    // The following excludes the deprecated kotlin-stdlib-jdk8 and kotlin-stdlib-jdk7
    // Since Kotlin 1.8.0, those are merged into kotlin-stdlib
    // Due to the way our library loader works, excluding these is required to prevent version conflicts
    dependencies {
        configurations.all {
            exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
            exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
        }
    }
    
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.xenondevs.xyz/releases")
        maven("https://repo.xenondevs.xyz/third-party-releases")
        maven("https://jitpack.io")
        maven("https://maven.enginehub.org/repo/")
        maven("https://repo.glaremasters.me/repository/bloodshot")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    
    tasks.withType<ProcessResources> {
        filesMatching(listOf("*.yml", "*.json")) {
            expand(project.properties)
        }
    }
    
    tasks.withType<KotlinJvmCompile>().all {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-opt-in=kotlin.io.path.ExperimentalPathApi"
            )
        }
    }
}