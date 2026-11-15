import org.jetbrains.kotlin.compiler.plugin.devkit.BetaAndRc

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
    // Nova enforces its Kotlin version for addons, so only that CLI compiler is needed
    val kotlinVersion = libs.versions.kotlin.get()
    cliVersions(kotlinVersion, kotlinVersion, BetaAndRc.NONE)
    // 2026.3+ is required for IDEA-254862 fix, so there's no need to build the compiler plugin for older versions of IDEA
    ideaVersions("263")

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
