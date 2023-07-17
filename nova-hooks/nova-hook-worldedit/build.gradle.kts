plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.9") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9") { isTransitive = false }
}