plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.detekt.plugin)
    implementation(origamiLibs.origami.plugin)

    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

gradlePlugin {
    plugins {
        create("bundler-jar-plugin") {
            id = "xyz.xenondevs.bundler-jar-plugin"
            implementationClass = "BundlerJarPlugin"
        }
    }
}
