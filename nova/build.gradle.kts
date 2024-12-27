plugins {
    java
    `maven-publish`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    id("xyz.xenondevs.bundler-plugin")
    alias(libs.plugins.paperweight)
}

dependencies {
    // server
    paperweight.paperDevBundle(libs.versions.paper)
    
    // api dependencies
    novaLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.invui.kotlin)
    novaLoaderApi(libs.joml.primitives)
    
    // internal dependencies
    compileOnly(project(":nova-api"))
    novaLoader(libs.bundles.ktor)
    novaLoader(libs.bundles.minecraft.assets)
    novaLoader(libs.bstats)
    novaLoader(libs.bytebase.runtime)
    novaLoader(libs.fuzzywuzzy)
    novaLoader(libs.awssdk.s3)
    novaLoader(libs.jimfs)
    novaLoader(libs.caffeine)
    novaLoader(libs.lz4)
    novaLoader(libs.zstd)
    novaLoader(libs.bundles.jgrapht)
    novaLoader(libs.snakeyaml.engine)
    novaLoader(libs.kotlinx.serialization.json)
    
    // test dependencies
    testImplementation(libs.bundles.test)
}

// configure java sources location
sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

tasks {
    // TODO: remove this workaround once the underlying bug is fixed
    getByName("compileJava").dependsOn(
        ":nova-hooks:nova-hook-fastasyncworldedit:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-griefprevention:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-itemsadder:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-luckperms:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-mmoitems:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-oraxen:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-plotsquared:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-protectionstones:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-quickshop:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-residence:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-towny:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-vault:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-worldedit:paperweightUserdevSetup",
        ":nova-hooks:nova-hook-worldguard:paperweightUserdevSetup"
    )
    
    withType<ProcessResources> {
        filesMatching("paper-plugin.yml") {
            val properties = HashMap(project.properties)
            properties["apiVersion"] = libs.versions.paper.get().substring(0, 4)
            expand(properties)
        }
    }
    
    test {
        useJUnitPlatform()
    }
}

// remove "dev" classifier set by paperweight-userdev
afterEvaluate {
    tasks.getByName<Jar>("jar") {
        archiveClassifier = ""
    }
}

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