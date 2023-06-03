plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://repo.glaremasters.me/repository/towny/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("com.palmergames.bukkit.towny:towny:0.99.0.4") { isTransitive = false }
}