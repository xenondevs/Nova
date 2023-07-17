plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    kotlin("jvm") version libs.versions.kotlin
}

dependencies {
    implementation(libs.bundles.minecraft.assets)
    implementation(libs.paper.api)
    implementation(libs.zip4j)
}

gradlePlugin {
    plugins {
        create("nova-gradle-plugin") {
            id = "xyz.xenondevs.nova.nova-gradle-plugin"
            description = "Nova gradle plugin to assist with addon development"
            implementationClass = "xyz.xenondevs.novagradle.NovaGradlePlugin"
        }
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
        create<MavenPublication>("novaGradlePlugin") {
            from(components.getByName("kotlin"))
            artifact(tasks.getByName("sources"))
        }
    }
}
