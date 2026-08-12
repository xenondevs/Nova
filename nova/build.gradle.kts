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

origami {
    paperDevBundle(libs.versions.paper.get())
    librariesDirectory = "lib"
    
    runServer {
        workingDirectory.set(layout.dir(providers.gradleProperty("serverDir").map(::File)))
        plugins.from(tasks.named<BuildBundlerJarTask>("loaderJar").flatMap { it.output })
        jvmArgs.addAll(
            "-XX:+EnableDynamicAgentLoading",
            "--enable-native-access=ALL-UNNAMED",
            "-DNovaDev",
            "-Dorigami.agent.loaded=true" // bypass agent check in NovaBootstrapper
        )
        
        // note: including Nova's libraries does not yield any improvement, rudimentary tests:
        // with novaLoader on application classpath: record+build: 241s exec: ~8.3 - 10s
        // w/o novaLoader on application classpath: record+build: 103s exec: ~8s
    }
}

val mcVersion = libs.versions.paper.map {
    val versionRegex = Regex("""(\d+\.\d+(?:\.\d+)?(?:-(?:rc|pre|snapshot)-\d+)?).*""")
    versionRegex.matchEntire(it)!!.groupValues[1]
}

val novaApiJar = project(":nova-api").tasks.withType<Jar>().matching { it.name == "jar" }
val hookJars = rootProject.subprojects
    .filter { it.name.startsWith("nova-hook-") }
    .map { hook -> hook.tasks.withType<Jar>().matching { it.name == "jar" } }

loaderJar {
    gameVersion = mcVersion
    novaInput = tasks.named<Jar>("origamiJar").flatMap { it.archiveFile }
    input.from(novaApiJar, hookJars)
}

val resourceProperties = mapOf(
    "version" to version.toString(),
    "apiVersion" to libs.versions.paper.get().substring(0, 4)
)

tasks {
    withType<ProcessResources> {
        inputs.properties(resourceProperties)
        filesMatching("paper-plugin.yml", ExpandPropertiesAction(resourceProperties))
    }
    test {
        environment("MINECRAFT_VERSION", mcVersion.get())
    }
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "kotlin.contracts.ExperimentalContracts",
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks.named<BuildBundlerJarTask>("loaderJar").flatMap { it.output }) {
                classifier = "loader"
                extension = "jar"
            }
        }
    }
}
