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
    compileOnly("com.bekvon:Residence:5.0.1.6") { isTransitive = false }
}