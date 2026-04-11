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
    api(project(":nova-registry"))
    api(project(":nova-config"))

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

// configure java sources location
sourceSets.main { java.setSrcDirs(listOf("src/main/kotlin/")) }

kotlin.sourceSets.main {
    kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/main/kotlin"))
}

val mcVersion = libs.versions.paper.map { 
    val versionRegex = Regex("""(\d+\.\d+(?:\.\d+)?).*""")
    versionRegex.matchEntire(it)!!.groupValues[1]
}

loaderJar {
    gameVersion = mcVersion
    novaInput = tasks.named<Jar>("origamiJar").flatMap { it.archiveFile }
    input.from(
        project.provider { project(":nova-api").tasks.named<Jar>("jar").map { it.archiveFile } },
        project.provider { project(":nova-config").tasks.named<Jar>("jar").map { it.archiveFile } },
        project.provider { project(":nova-registry").tasks.named<Jar>("jar").map { it.archiveFile } },
        project.provider {
            rootProject.subprojects
                .filter { it.name.startsWith("nova-hook-") }
                .map { hook -> hook.tasks.named<Jar>("jar").map { it.archiveFile } }
        }
    )
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
    file = tasks.named<BuildBundlerJarTask>("loaderJar").flatMap { it.output }
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