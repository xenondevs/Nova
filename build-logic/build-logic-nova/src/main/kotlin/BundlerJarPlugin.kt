
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.gradle.GenerateAnnotationIndex
import xyz.xenondevs.nova.annotations.gradle.MergeAnnotationIndexes
import xyz.xenondevs.origami.OrigamiPlugin
import java.io.File
import javax.inject.Inject

abstract class BundlerJarPlugin : Plugin<Project> {
    
    @get:Inject
    protected abstract val archives: ArchiveOperations
    
    override fun apply(project: Project) {
        project.pluginManager.apply(OrigamiPlugin::class.java)
        val origami = project.plugins.getPlugin(OrigamiPlugin::class.java)
        val providers = project.providers
        
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
        val mergedClasses = novaMergeClasspath.get().incoming.artifactView {
            attributes.attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                project.objects.named(LibraryElements.CLASSES)
            )
        }.artifacts
        val mergedResources = novaMergeClasspath.get().incoming.artifactView {
            attributes.attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                project.objects.named(LibraryElements.RESOURCES)
            )
        }.artifacts
        val mergedTrees = project.files(mergedClasses.artifactFiles, mergedResources.artifactFiles).elements.map { files ->
            files.map { file -> if (file.asFile.extension == "jar") archives.zipTree(file) else file }
        }
        val mergedAnnotationIndex = project.tasks.register<MergeAnnotationIndexes>("mergeAnnotationIndexes") {
            description = "Merges annotation indexes for the Nova loader JAR."
            indexes.from(project.tasks.named<GenerateAnnotationIndex>("generateAnnotationIndex").flatMap { it.outputFile })
            indexes.from(project.files(mergedTrees).asFileTree.matching { include(AnnotationIndex.FILE_NAME) })
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
                artifact.file to path
            }.sortedBy { it.second }.toMap()
        }
        
        val prepare = project.tasks.register<PrepareNovaLoaderTask>("prepareNovaLoader") {
            this.libraryPaths.set(libraryPaths.map { it.values.toList() })
            outputFile.set(project.layout.buildDirectory.file("novaLoader/nova-libraries"))
        }
        
        val ext = project.extensions.create<BuildLoaderJarExtension>("loaderJar")
        project.tasks.register<Jar>("loaderJar") {
            val origamiJar = project.tasks.getByName<Jar>("origamiJar")
            
            group = LifecycleBasePlugin.BUILD_GROUP
            
            duplicatesStrategy = DuplicatesStrategy.FAIL
            manifest.from(origamiJar.manifest)
            
            // nova-libraries index file
            from(prepare.flatMap { it.outputFile })
            
            // nova library jars
            inputs.property("libraryPaths", libraryPaths)
            from(libraryPaths.map { it.keys }) {
                eachFile { path = libraryPaths.get().getValue(file) }
            }
            
            // core nova classes + origami, origami libs, origami marker
            into("") {
                with(origamiJar)
                exclude(AnnotationIndex.FILE_NAME)
            }
            
            // classes from nova-api, nova-registry, etc.
            from(mergedTrees) {
                exclude(AnnotationIndex.FILE_NAME)
            }
            
            // merged nova-annotions.json
            from(mergedAnnotationIndex.flatMap { it.outputFile })
            
            val customOutDir = project.layout.dir(
                providers.gradleProperty("outDir")
                    .orElse(providers.systemProperty("outDir"))
                    .map(::File)
            )
            val outputDir = customOutDir.orElse(project.layout.buildDirectory)
            val novaVersion = providers.provider { project.version.toString() }
            val fileName = novaVersion.zip(ext.gameVersion) { nv, gv -> "Nova-$nv+MC-$gv.jar" }
            
            destinationDirectory.set(outputDir)
            archiveFileName.set(fileName)
        }
    }
    
}
