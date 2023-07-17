plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

dependencies {
    implementation(libs.paper.api)
    implementation(libs.zip4j)
    implementation(libs.specialsource)
    implementation(libs.stringremapper)
}

gradlePlugin {
    plugins {
        create("loader-jar-plugin") {
            id = "xyz.xenondevs.loader-jar-plugin"
            implementationClass = "LoaderJarPlugin"
        }
        create("library-loader-plugin") {
            id = "xyz.xenondevs.library-loader-plugin"
            implementationClass = "LibraryLoaderPlugin"
        }
    }
}