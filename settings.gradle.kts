rootProject.name = "nova"

// core
include("nova")
include("nova-api")
include("nova-config")
include("nova-network")
include("nova-registry")

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

// ksp
include("nova-ksp:annotations")
include("nova-ksp:processor:flatmap-extensions")
include("nova-ksp:processor:registry")
include("nova-ksp:processor:network")

// tooling
include("nova-detekt-rules")
include("nova-dokka-plugin")
include("nova-gradle-plugin")

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
            from("xyz.xenondevs.origami:origami-catalog:0.4.0") // !!! also change in build-logic !!!
        }
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") } }
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.xenondevs.xyz/releases/")
    }
}