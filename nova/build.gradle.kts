plugins {
    java
    `maven-publish`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    id("xyz.xenondevs.library-loader-plugin")
    alias(libs.plugins.paperweight)
}

dependencies {
    // server
    paperweight.paperDevBundle(libs.versions.paper)
    
    // api dependencies
    novaLoaderApi(variantOf(libs.nmsutilities) { classifier("remapped-mojang") })
    novaLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.invui.kotlin)
    
    // internal dependencies
    compileOnly(project(":nova-api"))
    compileOnly(project(":nova-loader"))
    novaLoader(libs.bundles.ktor)
    novaLoader(libs.bundles.minecraft.assets)
    novaLoader(libs.invui.resourcepack)
    novaLoader(variantOf(libs.inventoryaccess) { classifier("remapped-mojang") })
    novaLoader(libs.bstats)
    novaLoader(libs.bytbase.runtime)
    novaLoader(libs.fuzzywuzzy)
    novaLoader(libs.awssdk.s3)
    novaLoader(libs.jimfs)
    novaLoader(libs.caffeine)
    
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