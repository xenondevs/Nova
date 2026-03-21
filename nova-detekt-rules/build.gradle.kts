plugins {
    id("nova.kotlin-conventions")
    id("nova.publish-conventions-java")
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.test)
}