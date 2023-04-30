plugins {
    java
    `maven-publish`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    id("xyz.xenondevs.library-loader-plugin")
}

dependencies {
    // api dependencies
    spigotRuntimeApi(variantOf(libs.spigot.server) { classifier("remapped-mojang") })
    novaLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.bundles.kyori.adventure)
    novaLoaderApi(libs.invui.kotlin)
    novaLoaderApi("xyz.xenondevs:nms-utilities:0.9:remapped-mojang")
    
    // internal dependencies
    compileOnly(project(":nova-api"))
    compileOnly(project(":nova-loader"))
    novaLoader(libs.bundles.ktor)
    novaLoader(libs.bundles.minecraft.assets)
    novaLoader(libs.invui.resourcepack)
    novaLoader(variantOf(libs.inventoryaccess) { classifier("remapped-mojang") })
    novaLoader("xyz.xenondevs.bstats:bstats-bukkit:3.0.1")
    novaLoader("xyz.xenondevs.bytebase:ByteBase-Runtime:0.4.5")
    novaLoader("me.xdrop:fuzzywuzzy:1.4.0")
    novaLoader("software.amazon.awssdk:s3:2.18.35")
    novaLoader("com.google.jimfs:jimfs:1.2")
    novaLoader("com.github.ben-manes.caffeine:caffeine:3.1.6")
    
    // spigot runtime dependencies
    spigotRuntime(libs.bundles.maven.resolver)
    
    // test dependencies
    testImplementation(libs.bundles.test)
}

// configure java sources location
sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

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
        create<MavenPublication>("nova") {
            from(components.getByName("kotlin"))
            artifact(tasks.getByName("sources"))
        }
    }
}