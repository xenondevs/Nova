import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.provider.ValueSupplier.ValueProducer.task
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class JarLoaderPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val nova = project.project(":nova")
        val novaAPI = project.project(":nova-api")
        val novaLoader = project.project(":nova-loader")
        
        project.tasks.register<BuildLoaderJarTask>("loaderJar") {
            group = "build"
            
            dependsOn(listOf(
                nova.tasks.named("finalJar"),
                novaAPI.tasks.named("jar"),
                novaLoader.tasks.named("jar")
            ))
            
            this.nova = nova
            this.novaAPI = novaAPI
            this.novaLoader = novaLoader
        }
    }
    
}