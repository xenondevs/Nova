plugins {
    id("nova.publish-conventions")
    `version-catalog`
}

catalog {
    versionCatalog {
        version("kotlin", libs.versions.kotlin.get())
        version("nova", project.version.toString())
        val versionRegex = Regex("""(\d+\.\d+(?:\.\d+)?).*""")
        val paperVersion = libs.versions.paper.get()
        val mcVersion = versionRegex.matchEntire(paperVersion)!!.groupValues[1]
        version("paper", paperVersion)
        version("minecraft",  mcVersion)
        
        plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
        plugin("kotlinx.serialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
        plugin("nova", "xyz.xenondevs.nova.nova-gradle-plugin").versionRef("nova")
        
        library("nova", "xyz.xenondevs.nova", "nova").versionRef("nova")
        
        // plugin artifacts for cases like: https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
        library("kotlin-plugin", "org.jetbrains.kotlin", "kotlin-gradle-plugin").versionRef("kotlin")
        library("nova-plugin", "xyz.xenondevs.nova", "nova-gradle-plugin").versionRef("nova")
        val origamiPlugin = origamiLibs.origami.plugin.get()
        library("origami-plugin", origamiPlugin.group, origamiPlugin.name).version(origamiPlugin.version!!)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["versionCatalog"])
        }
    }
}