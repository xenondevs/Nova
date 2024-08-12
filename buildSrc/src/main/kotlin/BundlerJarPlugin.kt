
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class BundlerJarPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val nova = project.project(":nova")
        val novaAPI = project.project(":nova-api")
        val hooks = project.subprojects.filter { it.name.startsWith("nova-hook-") }
        
        project.tasks.register<BuildBundlerJarTask>("loaderJar") {
            this.group = "build"
            this.nova = nova
            this.novaApi = novaAPI
            this.hooks = hooks
            
            dependsOn(
                nova.tasks.named("classes"),
                novaAPI.tasks.named("classes"),
                hooks.map { it.tasks.named("classes") }
            )
        }
    }
    
}