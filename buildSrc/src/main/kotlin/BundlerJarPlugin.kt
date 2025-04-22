import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

class BundlerJarPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val bundlerJarExtension = project.extensions.create<BuildBundlerJarExtension>("loaderJar")
        project.tasks.register<BuildBundlerJarTask>("loaderJar") {
            this.group = LifecycleBasePlugin.BUILD_GROUP
            this.input = project.files(listOf(
                project.project(":nova"),
                project.project(":nova-api"),
                *project.subprojects.filter { it.name.startsWith("nova-hook-") }.toTypedArray()
            ).map { it.tasks.named<Jar>("jar").flatMap { it.archiveFile } })
            
            val customOutDir = (project.findProperty("outDir") as? String ?: System.getProperty("outDir"))
                ?.let { project.layout.dir(project.provider { File(it) }) }
                ?: project.layout.buildDirectory
            
            this.output.set(
                customOutDir.zip(bundlerJarExtension.gameVersion) { outDir, gameVersion ->
                    outDir.file("Nova-${project.version}+MC-$gameVersion.jar")
                }
            )
        }
    }
    
}