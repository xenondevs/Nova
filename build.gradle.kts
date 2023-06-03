plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    id("xyz.xenondevs.loader-jar-plugin")
}

fun RepositoryHandler.configureRepos() {
    mavenLocal()
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases")
    
    // include xenondevs-nms repository if requested
    if (project.hasProperty("xenondevsNms")) {
        maven("https://repo.papermc.io/repository/maven-public/") // authlib, brigadier, etc.
        maven {
            name = "xenondevsNms"
            url = uri("https://repo.xenondevs.xyz/nms/")
            credentials(PasswordCredentials::class)
        }
    }
}

repositories { configureRepos() }

loaderJar {
    spigotVersion.set(libs.versions.spigot)
}

subprojects {
    group = "xyz.xenondevs.nova"
    version = properties["version"] as String
    
    repositories { configureRepos() }
    
    // The following excludes the deprecated kotlin-stdlib-jdk8 and kotlin-stdlib-jdk7
    // Since Kotlin 1.8.0, those are merged into kotlin-stdlib
    // Due to the way our library loader works, excluding these is required to prevent version conflicts
    dependencies {
        configurations.all {
            exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
            exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
        }
    }
    
    tasks {
        register<Jar>("sources") {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            from("src/main/java", "src/main/kotlin")
            archiveClassifier.set("sources")
        }
        
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    "-Xjvm-default=all",
                    "-opt-in=kotlin.io.path.ExperimentalPathApi"
                )
            }
        }
    }
}