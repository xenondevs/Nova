description = "nova-api"

plugins {
    `maven-publish`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
}

dependencies {
    implementation(libs.bundles.kotlin)
    compileOnly(project(":nova-loader"))
    compileOnly(libs.spigot.api)
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