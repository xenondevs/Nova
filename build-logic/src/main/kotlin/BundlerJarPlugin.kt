
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Zip
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
        
        val mergedApiCfg = project.configurations.create("mergedApi")
        project.configurations.getByName("api").extendsFrom(mergedApiCfg)
        
        val mergedCfg = project.configurations.create("merged").apply { extendsFrom(mergedApiCfg) }
        project.configurations.getByName("implementation").extendsFrom(mergedCfg)
        
        val prepare = project.tasks.register<PrepareNovaLoaderTask>("prepareNovaLoader") {
            include.set(novaLoaderCfg)
            exclude.set(project.configurations.named("paperweightDevelopmentBundleCompileClasspath"))
            outputDir.set(project.layout.buildDirectory.dir("novaLoader"))
        }
        
        val ext = project.extensions.create<BuildLoaderJarExtension>("loaderJar")
        project.tasks.register<Zip>("loaderJar") {
            group = LifecycleBasePlugin.BUILD_GROUP
            
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(prepare.flatMap { it.outputDir })
            from(ext.merge.elements.map { jars -> jars.map { jar -> project.zipTree(jar) } })
            
            val customOutDir = (project.findProperty("outDir") as? String ?: System.getProperty("outDir"))
                ?.let { project.layout.dir(project.provider { File(it) }) }
                ?: project.layout.buildDirectory
            val fileName = ext.gameVersion.map { gameVersion -> "Nova-${project.version}+MC-$gameVersion.jar" }
            
            destinationDirectory.set(customOutDir)
            archiveFileName.set(fileName)
        }
    }
    
}