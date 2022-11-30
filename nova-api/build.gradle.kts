description = "nova-api"

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin
    `maven-publish`
}

dependencies {
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