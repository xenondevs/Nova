plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("dev.espi:protectionstones:2.10.2") { isTransitive = false }
}