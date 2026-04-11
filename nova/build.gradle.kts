plugins {
    id("nova.kotlin-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    id("nova.detekt-conventions")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    id("nova.origami-conventions")
    alias(libs.plugins.pluginPublish)
    id("xyz.xenondevs.bundler-jar-plugin")
}

dependencies {
    // ksp
    compileOnly(project(":nova-ksp:annotations"))
    ksp(project(":nova-ksp:processor:flatmap-extensions"))
    
    // api dependencies
    novaLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.invui.kotlin)
    novaLoaderApi(libs.joml.primitives)
    novaLoaderApi(libs.kotlinx.serialization.json)
    api(origamiLibs.mixin)
    api(origamiLibs.mixinextras)
    api(project(":nova-config"))
    api(project(":nova-registry"))
    api(project(":nova-network"))
    
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
    
    // test dependencies
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.test.junit)
    testRuntimeOnly(libs.junit.platformLauncher)
}

val mcVersion = libs.versions.paper.map {
    val versionRegex = Regex("""(\d+\.\d+(?:\.\d+)?).*""")
    versionRegex.matchEntire(it)!!.groupValues[1]
}

origami {
    transitiveAccessWidenerSources.from(configurations.named("runtimeClasspath"))
}

loaderJar {
    gameVersion = mcVersion
    merge.from(tasks.named<Jar>("origamiJar").flatMap { it.archiveFile })
    listOf(
        ":nova-api", 
        ":nova-config", 
        ":nova-network", 
        ":nova-registry",
    ).forEach { projectName ->
        merge.from(project.provider { project(projectName).tasks.named<Jar>("jar").flatMap { it.archiveFile } })
    }
}

tasks {
    withType<ProcessResources> {
        inputs.property("version", provider { version })
        inputs.property("apiVersion", libs.versions.paper)
        filesMatching("paper-plugin.yml") {
            expand(buildMap {
                put("version", version)
                put("apiVersion", libs.versions.paper.get().substring(0, 4))
            })
        }
    }
    test {
        environment("MINECRAFT_VERSION", mcVersion.get())
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlinx.coroutines.ExperimentalCoroutinesApi",
            "xyz.xenondevs.invui.ExperimentalReactiveApi",
            "xyz.xenondevs.invui.dsl.ExperimentalDslApi",
            "kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

pluginPublish {
    file = tasks.named<Zip>("loaderJar").flatMap { it.archiveFile }
    githubRepository = "xenondevs/Nova"
    discord()
    hangar("Nova") {
        gameVersions(mcVersion.get())
    }
    modrinth("yCVqpwUy") {
        gameVersions(mcVersion.get())
        incompatibleDependency("z4HZZnLr") // FastAsyncWorldEdit
    }
}