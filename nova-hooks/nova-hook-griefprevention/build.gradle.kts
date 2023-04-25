plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("com.github.TechFortress:GriefPrevention:16.17.1") { isTransitive = false }
}