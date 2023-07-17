plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

repositories {
    maven("https://repo.xenondevs.xyz/third-party-releases/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("org.maxgamer:QuickShop:5.1.0.7") { isTransitive = false }
}