import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

// Configures Kotlin compilations independently of the applied Kotlin plugin (jvm or multiplatform)

val isRelease = providers.gradleProperty("release").isPresent

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        optIn.addAll(
            "kotlin.io.path.ExperimentalPathApi",
            "kotlin.time.ExperimentalTime",
            "kotlin.experimental.ExperimentalTypeInference",
            "kotlin.contracts.ExperimentalContracts"
        )
        
        freeCompilerArgs.addAll(
            "-Xname-based-destructuring=complete",
            "-Xcollection-literals",
            "-Xcontext-sensitive-resolution"
        )
        
        if (!isRelease) {
            freeCompilerArgs.addAll(
                "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
            )
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
}
