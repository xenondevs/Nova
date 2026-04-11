dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") } }
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.xenondevs.xyz/releases/")
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("origamiLibs") {
            from("xyz.xenondevs.origami:origami-catalog:0.4.0") // !!! also change in root settings !!!
        }
    }
}
