import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.kotlin-conventions")
    id("dev.detekt")
}

val libs = the<LibrariesForLibs>()

dependencies {
    detektPlugins(project(":nova-detekt-rules"))
}

detekt {
    buildUponDefaultConfig = false
    allRules = false
    config.setFrom(rootProject.file("detekt.yml"))
}