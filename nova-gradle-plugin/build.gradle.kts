plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    alias(libs.plugins.kotlin)
}

dependencies {
    compileOnly(project(":nova"))
    implementation(libs.bundles.xenondevs.commons)
    implementation(libs.bundles.minecraft.assets)
    implementation(libs.configurate.yaml)
    implementation(libs.bytebase)
    compileOnly(libs.paper.api)
}

gradlePlugin {
    plugins {
        create("nova-gradle-plugin") {
            id = "xyz.xenondevs.nova.nova-gradle-plugin"
            description = "Gradle plugin for creating Nova addons"
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
