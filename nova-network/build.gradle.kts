plugins {
    id("nova.kotlin-conventions")
    id("nova.origami-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":nova-ksp:processor:network"))
}