plugins {
    id("nova.kotlin-conventions")
}

dependencies {
    compileOnly(libs.dokka.core)
    implementation(libs.dokka.base)
}