description = "nova-api"

plugins {
    kotlin("jvm") version "1.7.10"
    `maven-publish`
}

dependencies {
    compileOnly(deps.spigot.api)
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