plugins {
    id("nova.kotlin-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
}

dependencies {
    api(libs.paper.api)
    api(libs.commons.provider)
    api(libs.kotlinx.serialization.json)
    api(libs.cosmicBinaryFormat)
    ksp(project(":nova-ksp:processor:registry"))
    implementation(libs.commons.collections)
    testImplementation(libs.mockbukkit)
}

kotlin.sourceSets.main {
    kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/main/kotlin"))
}