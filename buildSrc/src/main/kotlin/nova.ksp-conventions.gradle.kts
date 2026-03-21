import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.kotlin-conventions")
}

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.ksp.api)
}
