
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.gradle.MergeAnnotationIndexes
import xyz.xenondevs.origami.OrigamiPlugin
import java.io.File

class BundlerJarPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        project.pluginManager.apply(OrigamiPlugin::class.java)
        val origami = project.plugins.getPlugin(OrigamiPlugin::class.java)
        
        val novaLoaderApiCfg = project.configurations.create("novaLoaderApi")
        project.configurations.getByName("api").extendsFrom(novaLoaderApiCfg)
        
        val novaLoaderCfg = project.configurations.create("novaLoader").apply { extendsFrom(novaLoaderApiCfg) }
        project.configurations.getByName("implementation").extendsFrom(novaLoaderCfg)
        
        val novaMergeCfg = project.configurations.dependencyScope("novaMerge")
        val novaMergeClasspath = project.configurations.resolvable("novaMergeClasspath") {
            extendsFrom(novaMergeCfg.get())
            isTransitive = false
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements.JAR))
            }
        }
        val mergedJars = project.files(
            project.tasks.named<Jar>("origamiJar").flatMap { it.archiveFile },
            novaMergeClasspath
        )
        val mergedAnnotationIndex = project.tasks.register<MergeAnnotationIndexes>("mergeAnnotationIndexes") {
            description = "Merges annotation indexes for the Nova loader JAR."
            jars.from(mergedJars)
            outputFile.set(project.layout.buildDirectory.file("loaderAnnotations/${AnnotationIndex.FILE_NAME}"))
        }
        
        val runtimeArtifacts = origami.serverRuntimeClasspath.incoming.artifacts.resolvedArtifacts
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
            from(mergedJars.elements.map { jars -> jars.map { jar -> project.zipTree(jar) } }) {
                exclude(AnnotationIndex.FILE_NAME)
            }
            from(mergedAnnotationIndex.flatMap { it.outputFile })
            
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
