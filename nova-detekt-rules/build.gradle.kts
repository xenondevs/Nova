plugins {
    id("nova.kotlin-conventions")
    id("nova.publish-conventions-java")
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.test) {
        // alpha.5 requests an unpublished detekt-api test-fixtures capability.
        exclude(group = "dev.detekt", module = "detekt-api")
    }
    testImplementation(libs.detekt.api)
}