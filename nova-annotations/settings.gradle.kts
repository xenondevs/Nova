rootProject.name = "nova-annotations"

include("nova-annotations-model")
include("nova-annotations-gradle-plugin")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {
    includeBuild("../build-logic/build-logic-core")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
