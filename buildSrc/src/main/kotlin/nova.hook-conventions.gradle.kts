import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.kotlin-conventions")
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly(libs.paper.api)
}