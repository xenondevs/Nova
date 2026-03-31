plugins {
    id("nova.dokka-conventions")
}

dependencies {
    dokka(project(":nova"))
    dokka(project(":nova-api"))
    dokka(project(":nova-config"))
    dokka(project(":nova-registry"))
    dokka(project(":nova-gradle-plugin"))
}