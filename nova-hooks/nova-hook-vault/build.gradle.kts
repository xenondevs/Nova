plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

repositories { 
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
}