plugins {
    id("nova.kotlin-conventions")
    id("nova.publish-conventions")
    id("nova.dokka-conventions")
    alias(libs.plugins.devkit.functionalTestPublishing)
    alias(libs.plugins.devkit.gradlePlugin)
    `kotlin-dsl`
}

dependencies {
    implementation("xyz.xenondevs.nova:nova-annotations-gradle-plugin:$version")
    implementation(libs.kotlin.plugin)
    implementation(libs.bundles.xenondevs.commons)
    implementation(libs.bytebase)
    implementation(origamiLibs.origami.plugin)
}

gradlePlugin {
    plugins {
        create("nova-gradle-plugin") {
            id = "xyz.xenondevs.nova.nova-gradle-plugin"
            description = "Gradle plugin for creating Nova addons"
            implementationClass = "xyz.xenondevs.novagradle.NovaGradlePlugin"
        }
    }
}

buildConfig {
    forClass("xyz.xenondevs.novagradle","Versions") {
        val novaVersion = "$version"
        val paperVersion = libs.versions.paper
        buildConfigField("NOVA", novaVersion)
        buildConfigField("NOVA_RELEASE", Regex("""^(\d+.\d+).*$""").matchEntire(novaVersion)!!.groupValues[1])
        buildConfigField("PAPER", paperVersion)
        buildConfigField("PAPER_API_VERSION", paperVersion.map { it.substring(0, 4) })
    }
}

pluginDevKit {
    pluginPackage = "xyz.xenondevs.nova.compiler"
    compilerPlugin(project(":nova-compiler-plugin"))
}
