plugins {
    `maven-publish`
    `version-catalog`
}

catalog {
    versionCatalog {
        version("kotlin", libs.versions.kotlin.get())
        version("paper", libs.versions.paper.get())
        version("paperweight", libs.versions.paperweight.get())
        version("nova", project.version.toString())
        version("minecraft", libs.versions.paper.get().substringBefore('-'))
        
        plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
        plugin("paperweight", "io.papermc.paperweight.userdev").versionRef("paperweight")
        plugin("nova", "xyz.xenondevs.nova.nova-gradle-plugin").versionRef("nova")
        
        library("nova", "xyz.xenondevs.nova", "nova").versionRef("nova")
        
        // plugin artifacts for cases like: https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
        library("kotlin-plugin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
        library("nova-plugin", "xyz.xenondevs.nova", "nova-gradle-plugin").versionRef("nova")
        library("paperweight-userdev-plugin", "io.papermc.paperweight", "paperweight-userdev").versionRef("paperweight")
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
        create<MavenPublication>("catalog") {
            from(components["versionCatalog"])
        }
    }
}