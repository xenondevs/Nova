plugins {
    id("nova.dokka-conventions")
}

dependencies {
    dokka(project(":nova"))
    dokka(project(":nova-api"))
}