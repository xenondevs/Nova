plugins {
    id("nova.ksp-conventions")
}

dependencies {
    implementation(project(":nova-ksp:annotations"))
    implementation(libs.kotlinpoet.ksp)
}