plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    compileOnly("net.luckperms:api:5.4") { isTransitive = false }
}