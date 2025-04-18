import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin

class BundlerJarPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val nova = project.project(":nova")
        val novaAPI = project.project(":nova-api")
        val hooks = project.subprojects.filter { it.name.startsWith("nova-hook-") }
        
        project.tasks.register<BuildBundlerJarTask>("loaderJar") {
            this.group = LifecycleBasePlugin.BUILD_GROUP
            this.input = project.files(listOf(
                project.project(":nova"),
                project.project(":nova-api"),
                *project.subprojects.filter { it.name.startsWith("nova-hook-") }.toTypedArray()
            ).map { it.tasks.named<Jar>("jar").flatMap { it.archiveFile } })
            
            val customOutDir = (project.findProperty("outDir") as? String)?.let(project::file)
                ?: System.getProperty("outDir")?.let(project::file)
                ?: project.layout.buildDirectory.get().asFile
            this.output.set(customOutDir.resolve("Nova-${project.version}.jar"))
            
            dependsOn(
                nova.tasks.named("classes"),
                novaAPI.tasks.named("classes"),
                hooks.map { it.tasks.named("classes") }
            )
        }
    }
    
}