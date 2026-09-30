plugins {
    `kotlin-dsl`
}

group = "xyz.xenondevs.nova.buildlogic"

dependencies {
    implementation(libs.kotlin.plugin)
    
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
