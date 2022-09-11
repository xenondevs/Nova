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
    
        val novaLoaderAPICfg = project.configurations.create("novaLoaderApi")
        val spigotLoaderAPICfg = project.configurations.create("spigotLoaderApi")
        project.configurations.getByName("api").extendsFrom(novaLoaderAPICfg, spigotLoaderAPICfg)
        
        val novaLoaderCfg = project.configurations.create("novaLoader").apply { extendsFrom(novaLoaderAPICfg) }
        val spigotLoaderCfg = project.configurations.create("spigotLoader").apply { extendsFrom(spigotLoaderAPICfg) }
        project.configurations.getByName("implementation").extendsFrom(novaLoaderCfg, spigotLoaderCfg)
    
        val spigotRuntimeCfg = project.configurations.create("spigotRuntime").apply { extendsFrom(spigotLoaderCfg) }
        project.configurations.getByName("compileOnly").extendsFrom(spigotRuntimeCfg)
    }
    
}