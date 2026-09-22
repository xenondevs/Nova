plugins {
    id("nova.kotlin-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    api(libs.commons.provider)
    api(libs.kotlinx.serialization.json)
    api(libs.adventure.key)
    api(libs.snakeyaml.engine)
    testImplementation(libs.jimfs)
}

kotlin {
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}
