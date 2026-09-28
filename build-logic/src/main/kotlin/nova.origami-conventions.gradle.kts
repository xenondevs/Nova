import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.kotlin-conventions")
    id("xyz.xenondevs.origami")
}

val libs = the<LibrariesForLibs>()
val origamiLibs = versionCatalogs.named("origamiLibs")

dependencies {
    compileOnly(origamiLibs.findLibrary("mixin").get())
    compileOnly(origamiLibs.findLibrary("mixinextras").get())
}

origami {
    paperDevBundle(libs.versions.paper.get())
    librariesDirectory = "lib"
}
