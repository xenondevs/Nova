plugins {
    `kotlin-dsl`
}

repositories { 
    mavenCentral()
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