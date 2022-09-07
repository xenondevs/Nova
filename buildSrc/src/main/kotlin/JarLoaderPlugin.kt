import org.gradle.api.Plugin
import org.gradle.api.Project
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
    
        val novaLoaderAPI = project.configurations.create("novaLoaderApi")
        val spigotLoaderAPI = project.configurations.create("spigotLoaderApi")
        project.configurations.getByName("api").extendsFrom(novaLoaderAPI, spigotLoaderAPI)
        
        val novaLoaderCfg = project.configurations.create("novaLoader").apply { extendsFrom(novaLoaderAPI) }
        val spigotLoaderCfg = project.configurations.create("spigotLoader").apply { extendsFrom(spigotLoaderAPI) }
        project.configurations.getByName("implementation").extendsFrom(novaLoaderCfg, spigotLoaderCfg)
    }
    
}