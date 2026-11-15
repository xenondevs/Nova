import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

fun KotlinCommonCompilerOptions.novaCompilerOptions(isRelease: Boolean) {
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