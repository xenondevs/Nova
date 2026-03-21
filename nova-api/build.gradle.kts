plugins {
    id("nova.java-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
}

dependencies {
    implementation("org.jetbrains:annotations:26.1.0")
    compileOnly(libs.paper.api)
}