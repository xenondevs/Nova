plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    implementation(project(":nova-api"))
    compileOnly("net.Indyuce:MMOItems:6.7") { isTransitive = false }
    compileOnly("io.lumine:MythicLib-dist:1.3") { isTransitive = false }
}