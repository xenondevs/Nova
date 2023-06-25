plugins {
    `maven-publish`
    `version-catalog`
}

catalog {
    versionCatalog {
        version("nova", project.version.toString())
        version("spigot", libs.versions.spigot.get())
        version("kotlin", libs.versions.kotlin.get())
        
        plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
        plugin("nova", "xyz.xenondevs.nova.nova-gradle-plugin").versionRef("nova")
        plugin("stringremapper", "xyz.xenondevs.string-remapper-gradle-plugin").version("1.3")
        plugin("specialsource", "xyz.xenondevs.specialsource-gradle-plugin").version("1.1")
        
        library("nova", "xyz.xenondevs.nova", "nova").versionRef("nova")
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