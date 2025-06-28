plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://repo.glaremasters.me/repository/towny/")
}

dependencies {
    compileOnly("com.palmergames.bukkit.towny:towny:0.99.0.4") { isTransitive = false }
}