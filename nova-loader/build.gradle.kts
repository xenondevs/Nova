plugins {
    java
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.maven.resolver)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    withType<ProcessResources> {
        filesMatching(listOf("*.yml")) {
            expand(project.properties)
        }
    }
}