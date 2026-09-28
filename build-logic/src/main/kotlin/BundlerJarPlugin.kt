import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
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
        
        val runtimeArtifacts = project.configurations
            .getByName("paperweightDevelopmentBundleRuntimeClasspath")
            .incoming.artifacts.resolvedArtifacts
        val libraryPaths = novaLoaderCfg.incoming.artifacts.resolvedArtifacts.zip(runtimeArtifacts) { libraries, runtime ->
            val runtimeModules = runtime.mapNotNullTo(HashSet()) { artifact ->
                (artifact.id.componentIdentifier as? ModuleComponentIdentifier)?.moduleIdentifier
            }
            libraries.mapNotNull { artifact ->
                val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier
                    ?: return@mapNotNull null
                if (id.moduleIdentifier in runtimeModules)
                    return@mapNotNull null
                
                val path = "lib/${id.group.replace('.', '/')}/${id.module}/${id.version}/${artifact.file.name}"
                artifact.file.absolutePath to path
            }.sortedBy { it.second }.toMap()
        }
        
        val prepare = project.tasks.register<PrepareNovaLoaderTask>("prepareNovaLoader") {
            libraries.from(libraryPaths.map { paths -> paths.keys.map(::File) })
            this.libraryPaths.set(libraryPaths)
            outputDir.set(project.layout.buildDirectory.dir("novaLoader"))
        }
        
        val ext = project.extensions.create<BuildLoaderJarExtension>("loaderJar")
        project.tasks.register<Zip>("loaderJar") {
            group = LifecycleBasePlugin.BUILD_GROUP
            
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            from(prepare.flatMap { it.outputDir })
            from(ext.merge.elements.map { jars -> jars.map { jar -> project.zipTree(jar) } })
            
            val customOutDir = project.layout.dir(
                project.providers.gradleProperty("outDir")
                    .orElse(project.providers.systemProperty("outDir"))
                    .map(::File)
            )
            val outputDir = customOutDir.orElse(project.layout.buildDirectory)
            val fileName = ext.gameVersion.map { gameVersion -> "Nova-${project.version}+MC-$gameVersion.jar" }
            
            destinationDirectory.set(outputDir)
            archiveFileName.set(fileName)
        }
    }
    
}
