plugins {
    id("nova.dokka-conventions")
    id("xyz.xenondevs.bundler-jar-plugin")
    alias(libs.plugins.pluginPublish)
}

dependencies {
    dokka(project(":nova"))
    dokka(project(":nova-api"))
}

loaderJar {
    gameVersion = libs.versions.paper.get().substringBefore('-')
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