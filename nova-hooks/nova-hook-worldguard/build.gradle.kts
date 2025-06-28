plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")
}