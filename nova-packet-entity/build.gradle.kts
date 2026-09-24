plugins {
    id("nova.kotlin-conventions")
    id("nova.origami-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    alias(libs.plugins.ksp)
}

dependencies {
    api(libs.commons.provider.dsl)
    api(project(":nova-interaction"))
    api(project(":nova-network"))
    api(libs.bundles.kotlin)
    implementation(libs.commons.collections)
    implementation(libs.commons.math)
    ksp(project(":nova-ksp:processor:packet-entity"))
}

ksp {
    arg("entityDataClasspath", configurations.named("compileClasspath").map { config ->
        config.files.joinToString(File.pathSeparator) { it.absolutePath }
    })
}
