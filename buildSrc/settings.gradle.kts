dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") } }
    }
    
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("origamiLibs") {
            from("xyz.xenondevs.origami:origami-catalog:0.1.0")
        }
    }
}