plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("dev.espi:protectionstones:2.10.2") { isTransitive = false }
}