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

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // versions
            version("kotlin", "1.8.20")
            version("ktor", "2.2.4")
            version("spigot", "1.19.4-R0.1-SNAPSHOT")
            version("cbf", "0.5")
            version("xenondevs-commons", "1.1")
            version("invui", "1.5")
            
            // plugins
            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("dokka", "org.jetbrains.dokka").version("1.8.10")
            
            // lib - kotlin
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            
            // lib - test
            library("kotlin-test-junit", "org.jetbrains.kotlin", "kotlin-test-junit").versionRef("kotlin")
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter:5.9.0")
            
            // lib - ktor
            library("ktor-server-core-jvm", "io.ktor", "ktor-server-core-jvm").versionRef("ktor")
            library("ktor-server-jetty-jvm", "io.ktor", "ktor-server-jetty-jvm").versionRef("ktor")
            library("ktor-client-core-jvm", "io.ktor", "ktor-client-core-jvm").versionRef("ktor")
            library("ktor-client-cio-jvm", "io.ktor", "ktor-client-cio-jvm").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-serialization-gson-jvm", "io.ktor", "ktor-serialization-gson-jvm").versionRef("ktor")
            
            // lib - spigot
            library("spigot-api", "org.spigotmc", "spigot-api").versionRef("spigot")
            library("spigot-server", "org.spigotmc", "spigot").versionRef("spigot")
            
            // lib - cbf
            library("cosmic-binary-format", "xyz.xenondevs.cbf", "cosmic-binary-format").versionRef("cbf")
            library("cosmic-binary-format-netty-adapter", "xyz.xenondevs.cbf", "cosmic-binary-format-netty-adapter").versionRef("cbf")
            
            // lib - xenondevs-commons
            library("commons-collections", "xyz.xenondevs.commons", "commons-collections").versionRef("xenondevs-commons")
            library("commons-gson", "xyz.xenondevs.commons", "commons-gson").versionRef("xenondevs-commons")
            library("commons-provider", "xyz.xenondevs.commons", "commons-provider").versionRef("xenondevs-commons")
            library("commons-reflection", "xyz.xenondevs.commons", "commons-reflection").versionRef("xenondevs-commons")
            
            // lib - maven resolver
            library("maven-resolver-provider", "org.apache.maven:maven-resolver-provider:3.8.5")
            library("maven-resolver-connector-basic", "org.apache.maven.resolver:maven-resolver-connector-basic:1.8.2")
            library("maven-resolver-transport-http", "org.apache.maven.resolver:maven-resolver-transport-http:1.8.2")
            
            // lib - minecraft assets
            library("minecraft-model-renderer", "xyz.xenondevs:minecraft-model-renderer:1.3")
            library("minecraft-asset-downloader", "xyz.xenondevs:minecraft-asset-downloader:1.3")
            library("resource-pack-obfuscator", "xyz.xenondevs:resource-pack-obfuscator:0.4.1")
            
            // lib - zip4j
            library("zip4j", "net.lingala.zip4j:zip4j:2.11.2")
            
            // lib - kyori adventure
            library("adventure-api", "net.kyori:adventure-api:4.12.0")
            library("adventure-text-serializer-gson", "net.kyori:adventure-text-serializer-gson:4.12.0")
            library("adventure-text-serializer-plain", "net.kyori:adventure-text-serializer-plain:4.12.0")
            library("adventure-platform-bukkit", "net.kyori:adventure-platform-bukkit:4.2.0")
            
            // lib - invui
            library("invui-kotlin", "xyz.xenondevs.invui", "invui-kotlin").versionRef("invui")
            library("invui-resourcepack", "xyz.xenondevs.invui", "invui-resourcepack").versionRef("invui")
            library("inventoryaccess", "xyz.xenondevs.invui", "inventory-access-r13").versionRef("invui")
            
            // bundles
            bundle("kotlin", listOf("kotlin-stdlib", "kotlin-reflect"))
            bundle("test", listOf("kotlin-test-junit", "junit-jupiter"))
            bundle("ktor", listOf("ktor-server-core-jvm", "ktor-server-jetty-jvm", "ktor-client-core-jvm", "ktor-client-cio-jvm", "ktor-client-content-negotiation", "ktor-serialization-gson-jvm"))
            bundle("cbf", listOf("cosmic-binary-format", "cosmic-binary-format-netty-adapter"))
            bundle("xenondevs-commons", listOf("commons-collections", "commons-gson", "commons-provider", "commons-reflection"))
            bundle("maven-resolver", listOf("maven-resolver-provider", "maven-resolver-connector-basic", "maven-resolver-transport-http"))
            bundle("minecraft-assets", listOf("minecraft-asset-downloader", "minecraft-model-renderer", "resource-pack-obfuscator"))
            bundle("kyori-adventure", listOf("adventure-api", "adventure-text-serializer-gson", "adventure-text-serializer-plain", "adventure-platform-bukkit"))
        }
    }
}

pluginManagement {
    repositories {
        mavenLocal()
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