description = "nova-api"

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("org.jetbrains.dokka") version libs.versions.dokka
    `maven-publish`
}

dependencies {
    implementation(libs.bundles.kotlin)
    compileOnly(project(":nova-loader"))
    compileOnly(libs.spigot.api)
}

tasks {
    register<Jar>("sources") {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        from("src/main/kotlin")
        archiveClassifier.set("sources")
    }
}

publishing {
    repositories {
        maven {
            credentials {
                name = "xenondevs"
                url = uri { "https://repo.xenondevs.xyz/releases/" }
                credentials(PasswordCredentials::class)
            }
        }
    }
    
    publications {
        create<MavenPublication>("novaAPI") {
            from(components.getByName("kotlin"))
            artifact(tasks.getByName("sources"))
        }
    }
}