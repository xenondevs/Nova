rootProject.name = "nova-parent"
include("nova")
include("nova-api")
include("nova-loader")
include("nova-gradle-plugin")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            // versions
            version("kotlin", "1.7.21")
            version("ktor", "2.1.3")
            version("spigot", "1.19.3-R0.1-SNAPSHOT")
            version("cbf", "0.2")
            
            // lib - kotlin
            library("kotlin-stdlib", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8").versionRef("kotlin")
            library("kotlin-reflect", "org.jetbrains.kotlin", "kotlin-reflect").versionRef("kotlin")
            
            // lib - test
            library("kotlin-test-junit", "org.jetbrains.kotlin", "kotlin-test-junit").versionRef("kotlin")
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter:5.9.0")
            
            // lib - ktor
            library("ktor-server-core-jvm", "io.ktor", "ktor-server-core-jvm").versionRef("ktor")
            library("ktor-server-netty-jvm", "io.ktor", "ktor-server-netty-jvm").versionRef("ktor")
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
            
            // lib - maven resolver
            library("maven-resolver-provider", "org.apache.maven:maven-resolver-provider:3.8.5")
            library("maven-resolver-connector-basic", "org.apache.maven.resolver:maven-resolver-connector-basic:1.8.2")
            library("maven-resolver-transport-http", "org.apache.maven.resolver:maven-resolver-transport-http:1.8.2")
            
            // lib - minecraft assets
            library("minecraft-model-renderer", "xyz.xenondevs:minecraft-model-renderer:1.2")
            library("minecraft-asset-downloader", "xyz.xenondevs:minecraft-asset-downloader:1.3")
            library("resource-pack-obfuscator", "xyz.xenondevs:resource-pack-obfuscator:0.4")
            
            // lib - zip4j
            library("zip4j", "net.lingala.zip4j:zip4j:2.11.2")
            
            // bundles
            bundle("kotlin", listOf("kotlin-stdlib", "kotlin-reflect"))
            bundle("test", listOf("kotlin-test-junit", "junit-jupiter"))
            bundle("ktor", listOf("ktor-server-core-jvm", "ktor-server-netty-jvm", "ktor-client-core-jvm", "ktor-client-cio-jvm", "ktor-client-content-negotiation", "ktor-serialization-gson-jvm"))
            bundle("cbf", listOf("cosmic-binary-format", "cosmic-binary-format-netty-adapter"))
            bundle("maven-resolver", listOf("maven-resolver-provider", "maven-resolver-connector-basic", "maven-resolver-transport-http"))
            bundle("minecraft-assets", listOf("minecraft-asset-downloader", "minecraft-model-renderer", "resource-pack-obfuscator"))
            
            // plugins
            plugin("kotlin", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
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