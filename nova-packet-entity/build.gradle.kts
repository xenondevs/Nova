plugins {
    id("nova.kotlin-conventions")
    id("nova.origami-conventions")
    id("nova.dokka-conventions")
    id("nova.publish-conventions-java")
    alias(libs.plugins.ksp)
}

dependencies {
    api(libs.commons.provider.dsl)
    implementation(libs.bundles.kotlin)
    implementation(project(":nova-network"))
    implementation(libs.commons.collections)
    ksp(project(":nova-ksp:processor:packet-entity"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xname-based-destructuring=complete")
    }
}

ksp {
    arg("entityDataClasspath", configurations.named("compileClasspath").map { config ->
        config.files.joinToString(File.pathSeparator) { it.absolutePath }
    })
}
