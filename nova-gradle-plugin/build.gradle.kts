plugins {
    id("nova.kotlin-conventions")
    id("nova.publish-conventions")
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    compileOnly(project(":nova"))
    implementation(libs.bundles.xenondevs.commons)
    implementation(libs.bundles.minecraft.assets)
    implementation(libs.configurate.yaml)
    implementation(libs.bytebase)
    compileOnly(libs.paper.api)
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