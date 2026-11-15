plugins {
    id("nova.kotlin-jvm-conventions")
    id("nova.publish-conventions-java")
    alias(libs.plugins.kotlinx.serialization)
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
