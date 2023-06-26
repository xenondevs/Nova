plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.xenondevs.xyz/releases/")
}

dependencies {
    implementation(libs.spigot.api)
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