import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("org.jetbrains.dokka") version "1.7.20"
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
    
    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf(
                "-Xjvm-default=all",
                "-opt-in=kotlin.io.path.ExperimentalPathApi"
            )
        }
    }
}