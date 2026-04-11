import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File

class BundlerJarPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val novaLoaderApiCfg = project.configurations.create("novaLoaderApi")
        project.configurations.getByName("api").extendsFrom(novaLoaderApiCfg)
        
        val novaLoaderCfg = project.configurations.create("novaLoader").apply { extendsFrom(novaLoaderApiCfg) }
        project.configurations.getByName("implementation").extendsFrom(novaLoaderCfg)
        
        val ext = project.extensions.create<BuildBundlerJarExtension>("loaderJar")
        project.tasks.register<BuildBundlerJarTask>("loaderJar") {
            this.group = LifecycleBasePlugin.BUILD_GROUP
            this.novaInput.set(ext.novaInput)
            this.input.from(ext.input)
            
            val customOutDir = (project.findProperty("outDir") as? String ?: System.getProperty("outDir"))
                ?.let { project.layout.dir(project.provider { File(it) }) }
                ?: project.layout.buildDirectory
            
            this.output.set(
                customOutDir.zip(ext.gameVersion) { outDir, gameVersion ->
                    outDir.file("Nova-${project.version}+MC-$gameVersion.jar")
                }
            )
        }
    }
    
}