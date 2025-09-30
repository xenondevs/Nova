plugins {
    id("nova.java-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions")
}

dependencies {
    implementation("org.jetbrains:annotations:26.0.2-1")
    compileOnly(libs.paper.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}