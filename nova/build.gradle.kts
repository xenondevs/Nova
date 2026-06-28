plugins {
    id("nova.kotlin-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    id("nova.detekt-conventions")
    alias(libs.plugins.kotlinx.serialization)
    id("nova.origami-conventions")
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
    api(project(":nova-config"))
    api(project(":nova-network"))
    api(project(":nova-packet-entity"))
    api(project(":nova-registry"))
    
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

origami {
    runServer {
        workingDirectory.set(layout.dir(providers.gradleProperty("serverDir").map(::File)))
        plugins.from(tasks.named<Zip>("loaderJar").flatMap { it.archiveFile })
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

origami {
    transitiveAccessWidenerSources.from(configurations.named("runtimeClasspath"))
}

loaderJar {
    gameVersion = mcVersion
    merge.from(tasks.named<Jar>("origamiJar").flatMap { it.archiveFile })
    val projectJars = listOf(
        ":nova-api", 
        ":nova-config", 
        ":nova-network", 
        ":nova-packet-entity",
        ":nova-registry",
    ).map { projectName -> project(projectName).tasks.withType<Jar>().matching { it.name == "jar" } }
    val hookJars = rootProject.subprojects
        .filter { it.name.startsWith("nova-hook-") }
        .map { hook -> hook.tasks.withType<Jar>().matching { it.name == "jar" } }
    merge.from(projectJars, hookJars)
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
publishing {
    publications {
        named<MavenPublication>("maven") {
            artifact(tasks.named<Zip>("loaderJar").flatMap { it.archiveFile }) {
                classifier = "loader"
                extension = "jar"
            }
        }
    }
}
