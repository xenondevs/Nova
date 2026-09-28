import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("nova.kotlin-base-conventions")
    id("nova.publish-conventions-java")
}

dependencies {
    compileOnly(libs.kotlin.compiler)
    testImplementation(libs.kotlin.compiler)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

tasks.test {
    jvmArgumentProviders.add(objects.newInstance<CompilerPluginArguments>().apply {
        pluginJar = tasks.jar.flatMap { it.archiveFile }
    })
}

abstract class CompilerPluginArguments : CommandLineArgumentProvider {
    
    @get:Classpath
    abstract val pluginJar: RegularFileProperty
    
    override fun asArguments(): Iterable<String> =
        listOf("-Dnova.compiler.plugin.jar=${pluginJar.get().asFile.absolutePath}")
}
