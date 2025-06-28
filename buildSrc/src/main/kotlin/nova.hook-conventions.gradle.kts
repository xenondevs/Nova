plugins {
    id("nova.kotlin-conventions")
    id("nova.paper-conventions")
}

dependencies {
    implementation(project(":nova"))
    implementation(project(":nova-api"))
}