plugins {
    id("nova.kotlin-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions")
    alias(libs.plugins.kotlinx.serialization)
    alias(origamiLibs.plugins.origami)
    alias(libs.plugins.pluginPublish)
    id("xyz.xenondevs.bundler-jar-plugin")
}

dependencies {
    // api dependencies
    novaLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.invui.kotlin)
    novaLoaderApi(libs.joml.primitives)
    novaLoaderApi(libs.kotlinx.serialization.json)
    api(origamiLibs.mixin)
    api(origamiLibs.mixinextras)
    
    // internal dependencies
    compileOnly(origami.patchedPaperServer())
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
    
    // test dependencies
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit)
    testRuntimeOnly(libs.junit.platformLauncher)
    testImplementation(origami.patchedPaperServer())
}

// configure java sources location
sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

origami {
    paperDevBundle(libs.versions.paper.get())
    librariesDirectory = "lib"
}

loaderJar {
    gameVersion = libs.versions.paper.get().substringBefore('-')
    novaInput = tasks.named<Jar>("origamiJar").flatMap { it.archiveFile }
    input.from(
        project.provider { project(":nova-api").tasks.named<Jar>("jar").map { it.archiveFile } } ,
        project.provider {
            rootProject.subprojects
                .filter { it.name.startsWith("nova-hook-") }
                .map { hook -> hook.tasks.named<Jar>("jar").map { it.archiveFile } }
        }
    )
}

tasks {
    withType<ProcessResources> {
        filesMatching("paper-plugin.yml") {
            val properties = HashMap(project.properties)
            properties["apiVersion"] = libs.versions.paper.get().substring(0, 4)
            expand(properties)
        }
    }
    test {
        environment("MINECRAFT_VERSION", libs.versions.paper.get().substringBefore("-R0.1-SNAPSHOT"))
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "xyz.xenondevs.invui.ExperimentalReactiveApi",
            "xyz.xenondevs.invui.dsl.ExperimentalDslApi"
        )
    }
}

pluginPublish {
    file = tasks.named<BuildBundlerJarTask>("loaderJar").flatMap { it.output }
    githubRepository = "xenondevs/Nova"
    discord()
    val gameVersion = libs.versions.paper.get().substringBefore('-')
    hangar("Nova") {
        gameVersions(gameVersion)
    }
    modrinth("yCVqpwUy") {
        gameVersions(gameVersion)
        incompatibleDependency("z4HZZnLr") // FastAsyncWorldEdit
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}