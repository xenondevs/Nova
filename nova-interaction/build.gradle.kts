plugins {
    id("nova.kotlin-conventions")
    id("nova.origami-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
}

dependencies {
    implementation(libs.invui.kotlin)
    implementation(project(":nova-network"))
}
