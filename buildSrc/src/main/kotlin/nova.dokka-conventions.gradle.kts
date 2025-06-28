plugins {
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

dependencies {
    dokkaPlugin(project(":nova-dokka-plugin"))
}