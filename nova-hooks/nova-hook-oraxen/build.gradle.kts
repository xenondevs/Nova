plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://repo.xenondevs.xyz/third-party-releases/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("io.th0rgal:oraxen:1.151.0") { isTransitive = false }
}