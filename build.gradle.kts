plugins {
    id("nova.dokka-conventions")
}

dependencies {
    dokka(project(":nova"))
    dokka(project(":nova-api"))
    dokka(project(":nova-config"))
    dokka(project(":nova-gradle-plugin"))
    dokka(project(":nova-interaction"))
    dokka(project(":nova-network"))
    dokka(project(":nova-packet-entity"))
    dokka(project(":nova-registry"))
}

for (taskName in listOf("publish", "publishToMavenLocal")) {
    tasks.register(taskName) {
        group = "publishing"
        description = "Publishes all Nova modules, including the annotation tooling."
        dependsOn(subprojects.map { subproject -> subproject.tasks.matching { it.name == taskName } })
        dependsOn(
            gradle.includedBuild("nova-annotations").task(":nova-annotations-model:$taskName"),
            gradle.includedBuild("nova-annotations").task(":nova-annotations-gradle-plugin:$taskName")
        )
    }
}
