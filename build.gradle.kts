plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.paperweight) apply false
    id("xyz.xenondevs.bundler-jar-plugin")
}

fun RepositoryHandler.configureRepos() {
    mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") }}
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

repositories { configureRepos() }

subprojects {
    group = "xyz.xenondevs.nova"
    version = properties["version"] as String
    
    repositories { configureRepos() }
    
    tasks {
        register<Jar>("sources") {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            from("src/main/java", "src/main/kotlin")
            archiveClassifier.set("sources")
        }
        
        withType<JavaCompile> {
            sourceCompatibility = "21"
            targetCompatibility = "21"
        }
        
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                
                freeCompilerArgs.addAll(
                    "-Xjvm-default=all", // Emit JVM default methods for interface declarations with bodies
                    
                    // experimental features
                    "-opt-in=kotlin.io.path.ExperimentalPathApi",
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=kotlin.experimental.ExperimentalTypeInference"
                )
                
                if (!project.hasProperty("release")) {
                    freeCompilerArgs.addAll(
                        "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
                    )
                }
            }
        }
    }
}