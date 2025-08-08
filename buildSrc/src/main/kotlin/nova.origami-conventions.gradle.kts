import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    id("nova.kotlin-conventions")
    id("xyz.xenondevs.origami")
}

val libs = the<LibrariesForLibs>()

dependencies {
    compileOnly(origami.patchedPaperServer())
}

origami {
    paperDevBundle(libs.versions.paper.get())
}

tasks.getByName<Jar>("jar") {
    addOrigamiLoader()
}