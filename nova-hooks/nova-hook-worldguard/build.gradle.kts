plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")
}