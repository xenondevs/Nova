plugins {
    id("nova.hook-conventions")
}

dependencies {
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.9") { isTransitive = false }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.9") { isTransitive = false }
}
