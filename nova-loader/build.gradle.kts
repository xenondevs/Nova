plugins {
    java
}

dependencies {
    compileOnly(libs.spigot.api)
    compileOnly(libs.bundles.maven.resolver)
}

tasks {
    withType<ProcessResources> {
        filesMatching(listOf("*.yml")) {
            expand(project.properties)
        }
    }
}
