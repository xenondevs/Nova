rootProject.name = "nova-parent"

// core project
include("nova")
include("nova-api")
include("nova-gradle-plugin")
include("nova-dokka-plugin")

// hooks
include("nova-hooks:nova-hook-griefprevention")
include("nova-hooks:nova-hook-itemsadder")
include("nova-hooks:nova-hook-luckperms")
include("nova-hooks:nova-hook-mmoitems")
include("nova-hooks:nova-hook-nexo")
include("nova-hooks:nova-hook-plotsquared")
include("nova-hooks:nova-hook-protectionstones")
include("nova-hooks:nova-hook-quickshop")
include("nova-hooks:nova-hook-residence")
include("nova-hooks:nova-hook-towny")
include("nova-hooks:nova-hook-vault")
include("nova-hooks:nova-hook-worldedit")
include("nova-hooks:nova-hook-worldguard")

// misc
include("catalog")

dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") } }
        mavenCentral()
        maven("https://repo.xenondevs.xyz/releases/")
    }
    
    versionCatalogs {
        create("libs")
        create("origamiLibs") {
            from("xyz.xenondevs.origami:origami-catalog:0.1.1")
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") } }
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.xenondevs.xyz/releases/")
    }
}