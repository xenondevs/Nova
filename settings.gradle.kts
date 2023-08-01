rootProject.name = "nova-parent"

// core project
include("nova")
include("nova-api")
include("nova-loader")
include("nova-gradle-plugin")

// hooks
include("nova-hooks:nova-hook-fastasyncworldedit")
include("nova-hooks:nova-hook-griefprevention")
include("nova-hooks:nova-hook-itemsadder")
include("nova-hooks:nova-hook-luckperms")
include("nova-hooks:nova-hook-mmoitems")
include("nova-hooks:nova-hook-oraxen")
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
    versionCatalogs {
        create("libs")
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.xenondevs.xyz/releases/")
    }
}

plugins {
    id("com.gradle.enterprise") version("3.13")
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}