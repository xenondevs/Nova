plugins {
    id("nova.kotlin-conventions")
    id("nova.publish-conventions")
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    implementation(libs.bundles.xenondevs.commons)
    implementation(libs.bundles.minecraft.assets)
    implementation(libs.configurate.yaml)
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

val generateVersionsClass = tasks.register("generateVersionsClass") {
    val generatedSrcDir = layout.buildDirectory.dir("generatedSrc")
    
    inputs.property("nova", provider { project.version })
    inputs.property("paper", libs.versions.paper)
    outputs.dir(generatedSrcDir)
    
    doLast {
        val src = generatedSrcDir.get().asFile.resolve("xyz/xenondevs/novagradle/Versions.kt")
        src.parentFile.mkdirs()
        src.writeText(
            """
            package xyz.xenondevs.novagradle
            
            internal object Versions {
                const val NOVA = "${project.version}"
                const val PAPER = "${libs.versions.paper.get()}"
            }
            """.trimIndent()
        )
    }
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generateVersionsClass)
        }
    }
}