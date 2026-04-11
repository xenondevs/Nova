import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("xyz.xenondevs.origami")
}

val libs = the<LibrariesForLibs>()

origami {
    paperDevBundle(libs.versions.paper.get())
    librariesDirectory = "lib"
}
