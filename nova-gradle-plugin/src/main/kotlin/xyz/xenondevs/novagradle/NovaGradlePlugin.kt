package xyz.xenondevs.novagradle

import addOrigamiJson
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.compiler.plugin.devkit.DevKitSupportPlugin
import xyz.xenondevs.nova.annotations.AnnotationIndex
import xyz.xenondevs.nova.annotations.gradle.AnnotationIndexExtension
import xyz.xenondevs.nova.annotations.gradle.AnnotationIndexPlugin
import xyz.xenondevs.nova.annotations.gradle.GenerateAnnotationIndex
import xyz.xenondevs.nova.compiler.PluginInfo
import xyz.xenondevs.novagradle.task.AddonExtension
import xyz.xenondevs.novagradle.task.GenerateLanguageFilesTask
import xyz.xenondevs.novagradle.task.PluginDependency
import xyz.xenondevs.novagradle.task.PluginDependency.Load
import xyz.xenondevs.novagradle.task.PluginDependency.Stage
import xyz.xenondevs.novagradle.task.PrepareAddonJar
import xyz.xenondevs.novagradle.task.SyncInjectables
import xyz.xenondevs.novagradle.util.toClassFilePath
import xyz.xenondevs.origami.extension.OrigamiExtension

private const val NOVA_TASK_GROUP = "nova"

private val MAVEN_CENTRAL_URLS = setOf(
    "https://repo1.maven.org/maven2",
    "http://repo1.maven.org/maven2",
    "https://repo.maven.apache.org/maven2",
    "http://repo.maven.apache.org/maven2"
)

internal class NovaGradlePlugin : DevKitSupportPlugin(PluginInfo.PLUGIN_INFO) {
    
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.jvm")
        target.pluginManager.apply("java-library")
        target.pluginManager.apply("xyz.xenondevs.origami")
        target.pluginManager.apply(AnnotationIndexPlugin::class.java)
        target.extensions.getByType(AnnotationIndexExtension::class.java).apply {
            classAnnotations.addAll(
                "xyz.xenondevs.nova.initialize.Init",
            )
            methodAnnotations.addAll(
                "xyz.xenondevs.nova.initialize.Init",
                "xyz.xenondevs.nova.initialize.InitFun",
                "xyz.xenondevs.nova.initialize.DisableFun"
            )
        }
        val addonExt = target.extensions.create<AddonExtension>("addon")
        val addonJar = createAddonJar(target, addonExt)
        configureOrigami(target, addonExt, addonJar)
        createGenLangFiles(target)
    }
    
    private fun configureOrigami(project: Project, ext: AddonExtension, addonJar: TaskProvider<Jar>) {
        val nova = project.configurations.detachedConfiguration(
            project.dependencyFactory.create("xyz.xenondevs.nova:nova:${Versions.NOVA}")
        )
        val novaLoader = project.configurations.detachedConfiguration(
            project.dependencyFactory.create("xyz.xenondevs.nova:nova:${Versions.NOVA}:loader")
        ).apply {
            isTransitive = false
        }
        val emptyFiles = project.providers.provider { emptyList<RegularFile>() }
        val runServerAddonJar = ext.addAddonJarToServerPlugins.flatMap { enabled ->
            if (enabled) addonJar.map { listOf(it.archiveFile.get()) }
            else emptyFiles
        }
        project.extensions.configure<OrigamiExtension>("origami") {
            devBundleVersion.set(Versions.PAPER)
            pluginId.set(ext.name.map { it.lowercase() })
            transitiveAccessWidenerSources.from(nova)
            runServer {
                plugins.from(novaLoader, runServerAddonJar)
                jvmArgs.addAll(
                    "-XX:+EnableDynamicAgentLoading",
                    "--enable-native-access=ALL-UNNAMED",
                    "-Dorigami.agent.loaded=true" // bypass agent check in NovaBootstrapper
                )
                jvmArgs.addAll(ext.novaDev.map { enabled ->
                    if (enabled) ["-ea", "-DNovaDev"]
                    else []
                })
            }
        }
    }
    
    private fun createAddonJar(project: Project, ext: AddonExtension): TaskProvider<Jar> {
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
        
        return project.tasks.register<Jar>("addonJar") {
            group = NOVA_TASK_GROUP
            
            archiveFileName.set(ext.fileName)
            destinationDirectory.set(ext.destination)
            with(project.copySpec {
                with(ext.input.get())
                exclude(AnnotationIndex.FILE_NAME)
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            })
            from(project.tasks.named<GenerateAnnotationIndex>("generateAnnotationIndex").flatMap { it.outputFile })
            from(prepAddonJar.map { it.output })
            addOrigamiJson()
        }
    }
    
    private fun createGenLangFiles(project: Project) {
        project.tasks.register<GenerateLanguageFilesTask>("generateLanguageFiles") {
            group = NOVA_TASK_GROUP
            languageDirectory.set(project.layout.projectDirectory.dir("src/main/resources/assets/lang/"))
        }
    }
    
}
