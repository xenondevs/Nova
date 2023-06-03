plugins {
    alias(libs.plugins.kotlin)
}

repositories { 
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}