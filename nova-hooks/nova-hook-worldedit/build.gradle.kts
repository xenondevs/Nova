plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.9") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9") { isTransitive = false }
}