plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    compileOnly("net.luckperms:api:5.4") { isTransitive = false }
}