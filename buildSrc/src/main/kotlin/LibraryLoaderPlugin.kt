import org.gradle.api.Plugin
import org.gradle.api.Project

class LibraryLoaderPlugin : Plugin<Project> {
    
    override fun apply(target: Project) {
        val novaLoaderAPICfg = target.configurations.create("novaLoaderApi")
        val spigotLoaderAPICfg = target.configurations.create("spigotLoaderApi")
        target.configurations.getByName("api").extendsFrom(novaLoaderAPICfg, spigotLoaderAPICfg)
        
        val novaLoaderCfg = target.configurations.create("novaLoader").apply { extendsFrom(novaLoaderAPICfg) }
        val spigotLoaderCfg = target.configurations.create("spigotLoader").apply { extendsFrom(spigotLoaderAPICfg) }
        target.configurations.getByName("implementation").extendsFrom(novaLoaderCfg, spigotLoaderCfg)
        
        val spigotRuntimeApiCfg = target.configurations.create("spigotRuntimeApi")
        target.configurations.getByName("api").extendsFrom(spigotRuntimeApiCfg)
        
        val spigotRuntimeCfg = target.configurations.create("spigotRuntime").apply { extendsFrom(spigotRuntimeApiCfg, spigotLoaderCfg) }
        target.configurations.getByName("compileOnly").extendsFrom(spigotRuntimeCfg)
    }
    
}