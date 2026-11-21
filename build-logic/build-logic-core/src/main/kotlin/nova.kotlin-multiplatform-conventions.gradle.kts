plugins {
    id("nova.common-conventions")
    id("nova.repository-conventions")
    id("nova.kotlin-compiler-conventions")
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}
