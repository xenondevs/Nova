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
    compileOnly("io.th0rgal:oraxen:1.157.2") { isTransitive = false }
}