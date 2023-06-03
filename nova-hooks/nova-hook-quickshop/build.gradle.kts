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
    compileOnly("org.maxgamer:QuickShop:5.1.0.7") { isTransitive = false }
}