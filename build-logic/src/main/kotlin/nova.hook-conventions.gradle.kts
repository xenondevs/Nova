import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.kotlin-conventions")
}

val libs = the<LibrariesForLibs>()

repositories {
    maven("https://repo.xenondevs.xyz/public/")
}

dependencies {
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly(libs.paper.api)
}
