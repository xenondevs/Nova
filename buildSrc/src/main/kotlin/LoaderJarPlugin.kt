
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class LoaderJarPlugin : Plugin<Project> { // Not really a loader anymore though
    
    override fun apply(project: Project) {
        val nova = project.project(":nova")
        val novaAPI = project.project(":nova-api")
        val hooks = project.subprojects.filter { it.name.startsWith("nova-hook-") }
        
        val extension = project.extensions.create<LoaderJarExtension>("loaderJar")
        
        fun BuildLoaderJarTask.configureCommons() {
            this.group = "build"
            this.nova = nova
            this.novaApi = novaAPI
            this.hooks = hooks
            this.gameVersion.set(extension.gameVersion)
            
            dependsOn(
                nova.tasks.named("classes"),
                novaAPI.tasks.named("classes"),
                hooks.map { it.tasks.named("classes") }
            )
        }
        
        project.tasks.register<BuildLoaderJarTask>("loaderJarMojang") {
            remap = false
            configureCommons()
        }
        
        project.tasks.register<BuildLoaderJarTask>("loaderJarSpigot") {
            remap = true
            configureCommons()
        }
    }
    
}