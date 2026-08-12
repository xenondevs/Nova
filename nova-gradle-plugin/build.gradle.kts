plugins {
    id("nova.kotlin-conventions")
    id("nova.publish-conventions")
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.bundles.xenondevs.commons)
    implementation(libs.bundles.minecraft.assets)
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

val generateVersionsClass = tasks.register<GenerateVersionsClassTask>("generateVersionsClass") {
    novaVersion.set(version.toString())
    paperVersion.set(libs.versions.paper)
    outputDirectory.set(layout.buildDirectory.dir("generatedSrc"))
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generateVersionsClass)
        }
    }
}