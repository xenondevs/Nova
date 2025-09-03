plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") } }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.paperweight.userdev.plugin)
    
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        create("bundler-jar-plugin") {
            id = "xyz.xenondevs.bundler-jar-plugin"
            implementationClass = "BundlerJarPlugin"
        }
        create("bundler-plugin") {
            id = "xyz.xenondevs.bundler-plugin"
            implementationClass = "BundlerPlugin"
        }
    }
}