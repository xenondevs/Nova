plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.9") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9") { isTransitive = false }
}