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
    configurations.getByName("mojangMappedServer").apply {
        exclude("org.spongepowered", "configurate-yaml")
    }
    
    // api dependencies
    prioritizedNovaLoaderApi(libs.bundles.configurate)
    novaLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.invui.kotlin)
    novaLoaderApi(libs.joml.primitives)
    
    // internal dependencies
    compileOnly(project(":nova-loader"))
    compileOnly(project(":nova-api"))
    novaLoader(libs.bundles.ktor)
    novaLoader(libs.bundles.minecraft.assets)
    novaLoader(libs.inventoryaccess)
    novaLoader(libs.bstats)
    novaLoader(libs.bytbase.runtime)
    novaLoader(libs.fuzzywuzzy)
    novaLoader(libs.awssdk.s3)
    novaLoader(libs.jimfs)
    novaLoader(libs.caffeine)
    novaLoader(libs.lz4)
    novaLoader(libs.zstd)
    novaLoader(libs.bundles.jgrapht)
    novaLoader(libs.snakeyaml.engine)
    
    // runtime dependencies
    spigotRuntime(paperweight.paperDevBundleDependency(libs.versions.paper.get()))
    spigotRuntime(libs.bundles.maven.resolver)
    
    // test dependencies
    testImplementation(libs.bundles.test)
}

// configure java sources location
sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

tasks {
    withType<ProcessResources> {
        filesMatching("paper-plugin.yml") {
            expand(project.properties)
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