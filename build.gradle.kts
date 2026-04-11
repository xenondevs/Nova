plugins {
    id("nova.dokka-conventions")
}

dependencies {
    dokka(project(":nova"))
    dokka(project(":nova-api"))
    dokka(project(":nova-config"))
    dokka(project(":nova-gradle-plugin"))
    dokka(project(":nova-network"))
    dokka(project(":nova-registry"))
}