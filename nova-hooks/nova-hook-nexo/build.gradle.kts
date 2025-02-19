plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

repositories {
    maven("https://repo.nexomc.com/releases")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("com.nexomc:nexo:1.0.0")
}