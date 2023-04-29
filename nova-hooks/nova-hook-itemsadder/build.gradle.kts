plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.2.4") { isTransitive = false }
}