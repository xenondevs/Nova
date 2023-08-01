import org.gradle.api.Plugin
import org.gradle.api.Project

class LibraryLoaderPlugin : Plugin<Project> {
    
    override fun apply(target: Project) {
        val novaLoaderApiCfg = target.configurations.create("novaLoaderApi")
        val prioritizedNovaLoaderApiCfg = target.configurations.create("prioritizedNovaLoaderApi")
        val spigotLoaderApiCfg = target.configurations.create("spigotLoaderApi")
        val spigotRuntimeApiCfg = target.configurations.create("spigotRuntimeApi")
        target.configurations.getByName("api").extendsFrom(novaLoaderApiCfg, prioritizedNovaLoaderApiCfg, spigotLoaderApiCfg, spigotRuntimeApiCfg)
        
        val novaLoaderCfg = target.configurations.create("novaLoader").apply { extendsFrom(novaLoaderApiCfg) }
        val prioritizedNovaLoaderCfg = target.configurations.create("prioritizedNovaLoader").apply { extendsFrom(prioritizedNovaLoaderApiCfg) }
        val spigotLoaderCfg = target.configurations.create("spigotLoader").apply { extendsFrom(spigotLoaderApiCfg) }
        target.configurations.getByName("implementation").extendsFrom(novaLoaderCfg, spigotLoaderCfg, prioritizedNovaLoaderCfg)
        
        val spigotRuntimeCfg = target.configurations.create("spigotRuntime").apply { extendsFrom(spigotRuntimeApiCfg, spigotLoaderCfg) }
        target.configurations.getByName("compileOnly").extendsFrom(spigotRuntimeCfg)
    }
    
}