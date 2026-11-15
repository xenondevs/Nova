plugins {
    id("nova.kotlin-jvm-conventions")
    id("nova.publish-conventions")
    `java-gradle-plugin`
}

dependencies {
    api(project(":nova-annotations-model"))
    implementation(libs.asm)
    compileOnly(gradleKotlinDsl())
}

gradlePlugin {
    plugins {
        create("nova-annotations") {
            id = "xyz.xenondevs.nova.annotations"
            implementationClass = "xyz.xenondevs.nova.annotations.gradle.AnnotationIndexPlugin"
        }
    }
}
