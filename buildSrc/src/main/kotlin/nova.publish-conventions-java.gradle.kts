plugins {
    id("nova.publish-conventions")
    id("nova.java-conventions")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}