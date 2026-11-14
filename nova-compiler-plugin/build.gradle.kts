plugins {
    id("nova.publish-conventions")
    pluginDevKit("compiler-plugin")
}

kotlin {
    compilerOptions { novaCompilerOptions(providers.gradleProperty("release").isPresent) }
    sourceSets {
        commonTestFixtures.dependencies {
            implementation(dependencies.platform(libs.junit.bom))
            api(libs.junit.jupiter)
            api(libs.junit.platformLauncher)
            api(libs.slf4j.simple)
        }
    }
}

pluginDevKit {
    cliVersions("2.3.0")
    ideaVersions("253")

    pluginPackage = "xyz.xenondevs.nova.compiler"
    testDataLibraries {
        register("paperApi") { jvm(libs.paper.api) }
        register("novaRegistry") { jvm(project(":nova-registry")) }
    }
    componentRegistrar =
        "xyz.xenondevs.nova.compiler.NovaCompilerPluginRegistrar"
    commandLineProcessor =
        "xyz.xenondevs.nova.compiler.NovaCommandLineProcessor"
}
