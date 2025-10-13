package xyz.xenondevs.novagradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import xyz.xenondevs.novagradle.task.AddonExtension
import xyz.xenondevs.novagradle.task.GenerateLanguageFilesTask
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesExtension
import xyz.xenondevs.novagradle.task.GenerateWailaTexturesTask
import xyz.xenondevs.novagradle.task.PluginDependency
import xyz.xenondevs.novagradle.task.PluginDependency.Load
import xyz.xenondevs.novagradle.task.PluginDependency.Stage
import xyz.xenondevs.novagradle.task.PrepareAddonJar
import xyz.xenondevs.novagradle.task.SyncInjectables
import xyz.xenondevs.novagradle.util.toClassFilePath
import xyz.xenondevs.origami.extension.OrigamiExtension
import xyz.xenondevs.origami.task.packaging.PrepareOrigamiMarkerTask

private const val NOVA_TASK_GROUP = "nova"

private val MAVEN_CENTRAL_URLS = setOf(
    "https://repo1.maven.org/maven2",
    "http://repo1.maven.org/maven2",
    "https://repo.maven.apache.org/maven2",
    "http://repo.maven.apache.org/maven2"
);

internal class NovaGradlePlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val addonExt = project.extensions.create<AddonExtension>("addon")
        configureOrigami(project, addonExt)
        createAddonJar(project, addonExt)
        createGenWailaTextures(project, addonExt)
        createGenLangFiles(project)
    }
    
    private fun configureOrigami(project: Project, ext: AddonExtension) {
        project.pluginManager.apply("xyz.xenondevs.origami")
        project.extensions.configure<OrigamiExtension>("origami") {
            devBundleVersion.set(Versions.PAPER)
            pluginId.set(ext.name.map { it.lowercase() })
            transitiveAccessWidenerSources.from(
                project.configurations.detachedConfiguration(
                    project.dependencyFactory.create("xyz.xenondevs.nova:nova:${Versions.NOVA}")
                )
            )
        }
    }
    
    private fun createAddonJar(project: Project, ext: AddonExtension) {
        val libraryLoaderApiCfg = project.configurations.register("libraryLoaderApi") {
            isCanBeDeclared = true
            isCanBeResolved = false
            isCanBeConsumed = false
        }
        project.configurations.named("api").configure { extendsFrom(libraryLoaderApiCfg.get()) }
        
        val libraryLoaderCfg = project.configurations.register("libraryLoader") {
            isCanBeDeclared = true
            isCanBeResolved = false
            isCanBeConsumed = false
        }
        project.configurations.named("implementation").configure { extendsFrom(libraryLoaderCfg.get()) }
        
        val extractInjectables = project.tasks.register<SyncInjectables>("_novaSyncInjectables") {
            bootstrapper.set(ext.bootstrapper.map(String::toClassFilePath))
            loader.set(ext.loader.map(String::toClassFilePath))
            outputDir.set(project.layout.buildDirectory.dir("nova/addon-injectables"))
            with(ext.input.get())
        }
        
        val prepAddonJar = project.tasks.register<PrepareAddonJar>("_novaPrepareAddonJar") {
            addonName.set(ext.name)
            main.set(ext.main)
            pluginMain.set(ext.pluginMain)
            loader.set(ext.loader)
            loaderFile.set(extractInjectables.flatMap { it.loaderFile })
            bootstrapper.set(ext.bootstrapper)
            bootstrapperFile.set(extractInjectables.flatMap { it.bootstrapperFile })
            version.set(ext.version)
            dependencies.set(ext.dependencies)
            dependencies.add(PluginDependency("Nova", Stage.BOOTSTRAP, Load.BEFORE, required = true, joinClasspath = true))
            dependencies.add(PluginDependency("Nova", Stage.SERVER, Load.BEFORE, required = true, joinClasspath = true))
            addonDescription.set(ext.description)
            authors.set(ext.authors)
            contributors.set(ext.contributors)
            website.set(ext.website)
            prefix.set(ext.prefix)
            
            libraryLoaderRepositoryUrls.set(
                project.repositories.withType<MavenArtifactRepository>().asSequence()
                    .filter { it.url.scheme != "file" }
                    .map { it.url.toString() }
                    .filter { url -> MAVEN_CENTRAL_URLS.none { centralUrl -> url.startsWith(centralUrl) } }
                    .toCollection(LinkedHashSet()) // deduplicate
                    .toList()
            )
            
            libraryLoaderDependencyCoordinates.set(
                libraryLoaderCfg
                    .zip(libraryLoaderApiCfg) { cfg, apiCfg -> cfg.dependencies + apiCfg.dependencies }
                    .map { deps ->
                        deps.filterIsInstance<ExternalModuleDependency>()
                            .map { dependency ->
                                val artifact = dependency.artifacts.firstOrNull()
                                if (artifact != null)
                                    "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
                                else "${dependency.group}:${dependency.name}:${dependency.version}"
                            }
                            .toList()
                    }
            )
            
            output.set(project.layout.buildDirectory.dir("nova/addon"))
        }
        
        project.tasks.register<Jar>("addonJar") {
            group = NOVA_TASK_GROUP
            
            archiveFileName.set(ext.fileName)
            destinationDirectory.set(ext.destination)
            with(project.copySpec {
                with(ext.input.get())
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            })
            from(prepAddonJar.map { it.output })
            from(project.tasks.named<PrepareOrigamiMarkerTask>("_oriPrepareMarker").flatMap { it.jsonOutput })
        }
    }
    
    private fun createGenWailaTextures(project: Project, addonExt: AddonExtension) {
        val wailaExt = project.extensions.create<GenerateWailaTexturesExtension>("generateWailaTextures")
        
        project.tasks.register<GenerateWailaTexturesTask>("generateWailaTextures") {
            group = NOVA_TASK_GROUP
            resourcesDir.set(wailaExt.resourcesDir.orElse(project.layout.projectDirectory.dir("src/main/resources/")))
            addonId.set(addonExt.name.map { it.lowercase() })
            filter.set(wailaExt.filter.orElse { true })
        }
    }
    
    private fun createGenLangFiles(project: Project) {
        project.tasks.register<GenerateLanguageFilesTask>("generateLanguageFiles") {
            group = NOVA_TASK_GROUP
        }
    }
    
}