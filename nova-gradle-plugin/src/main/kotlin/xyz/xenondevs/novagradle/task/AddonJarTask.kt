package xyz.xenondevs.novagradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.MethodNode
import xyz.xenondevs.bytebase.util.insertBeforeEvery
import xyz.xenondevs.novagradle.Versions
import xyz.xenondevs.novagradle.util.TaskUtils
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.writeBytes

private const val PAPER_PLUGIN_YML = "paper-plugin.yml"
private const val NOVA_ADDON_YML = "nova-addon.yml"
private const val ORIGAMI_MARKER = "origami.json"
private const val JAVA_PLUGIN_NAME = "xyz.xenondevs.addonloader.JavaPlugin"
private const val BOOTSTRAPPER_NAME = "xyz.xenondevs.addonloader.Bootstrapper"
private const val PLUGIN_LOADER_NAME = "xyz.xenondevs.addonloader.PluginLoader"
private val JAVA_PLUGIN_INTERNAL_NAME = JAVA_PLUGIN_NAME.replace('.', '/')
private val BOOTSTRAPPER_INTERNAL_NAME = BOOTSTRAPPER_NAME.replace('.', '/')
private val PLUGIN_LOADER_INTERNAL_NAME = PLUGIN_LOADER_NAME.replace('.', '/')
private val JAVA_PLUGIN_FILE_PATH = "$JAVA_PLUGIN_INTERNAL_NAME.class"
private val PLUGIN_LOADER_FILE_PATH = "$PLUGIN_LOADER_INTERNAL_NAME.class"

@DisableCachingByDefault
abstract class AddonJarTask : DefaultTask() {
    
    @get:Input
    abstract val addonName: Property<String>
    
    @get:Input
    abstract val version: Property<String>
    
    @get:Input
    abstract val main: Property<String>
    
    @get:Input
    @get:Optional
    abstract val pluginMain: Property<String>
    
    @get:Input
    @get:Optional
    abstract val bootstrapper: Property<String>
    
    @get:Input
    @get:Optional
    abstract val loader: Property<String>
    
    @get:Input
    abstract val dependencies: ListProperty<PluginDependency>
    
    @get:Input
    @get:Optional
    abstract val addonDescription: Property<String>
    
    @get:Input
    @get:Optional
    abstract val authors: ListProperty<String>
    
    @get:Input
    @get:Optional
    abstract val contributors: ListProperty<String>
    
    @get:Input
    @get:Optional
    abstract val website: Property<String>
    
    @get:Input
    @get:Optional
    abstract val prefix: Property<String>
    
    @get:InputFile
    abstract val input: RegularFileProperty
    
    @get:InputFile
    abstract val origamiMarker: RegularFileProperty
    
    @get:OutputFile
    abstract val output: RegularFileProperty
    
    @TaskAction
    fun run() {
        val (novaVersion, apiVersion) = TaskUtils.readNovaAndApiVersion(project)
        
        val input = input.asFile.get().toPath()
        val output = output.asFile.get().toPath()
        
        val tmp = input.parent.resolve(input.name + ".tmp")
        input.copyTo(tmp, true)
        
        FileSystems.newFileSystem(tmp).use {
            val root = it.rootDirectories.first()
            
            writePaperPluginYml(apiVersion, root.resolve(PAPER_PLUGIN_YML))
            writeNovaAddonYml(novaVersion, root.resolve(NOVA_ADDON_YML))
            writeOrigamiMarker(root.resolve(ORIGAMI_MARKER))
            generateBootstrapper(root.resolve(bootstrapper.getOrElse(BOOTSTRAPPER_NAME).replace('.', '/') + ".class"))
            if (!pluginMain.isPresent)
                generateJavaPlugin(root.resolve(JAVA_PLUGIN_FILE_PATH))
            if (!loader.isPresent) // only generate loader if no custom loader is specified
                generateLoader(root.resolve(PLUGIN_LOADER_FILE_PATH))
        }
        
        // this approach allows overwriting files with open file handles on Windows
        tmp.inputStream().use { inp -> output.outputStream().use { out -> inp.transferTo(out) } }
        tmp.deleteExisting()
    }
    
    private fun writePaperPluginYml(apiVersion: String, path: Path) {
        val pluginYmlLoader = YamlConfigurationLoader.builder().path(path).build()
        val pluginYml = pluginYmlLoader.load()
        
        // generic metadata
        pluginYml.node("name").set(addonName.get())
        pluginYml.node("version").set(version.get())
        pluginYml.node("api-version").set(apiVersion)
        if (addonDescription.isPresent) {
            pluginYml.node("description").set(addonDescription.get())
        }
        if (authors.isPresent) {
            pluginYml.node("authors").set(authors.get())
        }
        if (contributors.isPresent) {
            pluginYml.node("contributors").set(contributors.get())
        }
        if (website.isPresent) {
            pluginYml.node("website").set(website.get())
        }
        if (prefix.isPresent) {
            pluginYml.node("prefix").set(prefix.get())
        }
        
        // main, loader, bootstrapper
        pluginYml.node("main").set(pluginMain.getOrElse(JAVA_PLUGIN_NAME))
        pluginYml.node("loader").set(loader.getOrElse(PLUGIN_LOADER_NAME))
        pluginYml.node("bootstrapper").set(bootstrapper.getOrElse(BOOTSTRAPPER_NAME))
        
        // dependencies    
        for (dependency in dependencies.get()) {
            val pluginNode = pluginYml.node("dependencies", dependency.stage.name.lowercase(), dependency.name)
            pluginNode.node("load").set(dependency.load)
            pluginNode.node("required").set(dependency.required)
            pluginNode.node("join-classpath").set(dependency.joinClasspath)
        }
        
        pluginYmlLoader.save(pluginYml)
    }
    
    private fun writeNovaAddonYml(novaVersion: String, path: Path) {
        val addonYmlLoader = YamlConfigurationLoader.builder().path(path).build()
        val addonYml = addonYmlLoader.load()
        
        addonYml.node("main").set(main.get())
        addonYml.node("nova_version").set(novaVersion)
        
        addonYmlLoader.save(addonYml)
    }
    
    private fun writeOrigamiMarker(path: Path) {
        origamiMarker.asFile.get().toPath().copyTo(path, true)
    }
    
    private fun generateJavaPlugin(path: Path) {
        if (path.exists())
            return
        
        val javaPlugin = ClassNode(Opcodes.ASM9).apply {
            version = Opcodes.V21
            name = JAVA_PLUGIN_INTERNAL_NAME
            access = Opcodes.ACC_PUBLIC
            superName = "org/bukkit/plugin/java/JavaPlugin"
            
            methods = mutableListOf(
                // <init>
                MethodNode(
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V"
                ) {
                    addLabel()
                    aLoad(0)
                    invokeSpecial("org/bukkit/plugin/java/JavaPlugin", "<init>", "()V")
                    _return()
                }
            )
        }
        
        val bin = ClassWriter(ClassWriter.COMPUTE_FRAMES).also(javaPlugin::accept).toByteArray()
        path.parent.createDirectories()
        path.writeBytes(bin)
    }
    
    private fun generateBootstrapper(path: Path) {
        val bootstrapper = ClassNode(Opcodes.ASM9)
        
        // default bootstrap (empty)
        val defaultBootstrap = MethodNode(
            Opcodes.ACC_PUBLIC,
            "bootstrap",
            "(Lio/papermc/paper/plugin/bootstrap/BootstrapContext;)V"
        ) {
            addLabel()
            _return()
        }
        
        // default createPlugin (calls interface default)
        val defaultCreatePlugin = MethodNode(
            Opcodes.ACC_PUBLIC,
            "createPlugin",
            "(Lio/papermc/paper/plugin/bootstrap/PluginProviderContext;)Lorg/bukkit/plugin/java/JavaPlugin;"
        ) {
            addLabel()
            aLoad(0)
            aLoad(1)
            invokeSpecial(
                "io/papermc/paper/plugin/bootstrap/PluginBootstrap",
                "createPlugin",
                "(Lio/papermc/paper/plugin/bootstrap/PluginProviderContext;)Lorg/bukkit/plugin/java/JavaPlugin;",
                isInterface = true
            )
            areturn()
        }
        
        if (path.notExists()) {
            bootstrapper.apply {
                version = Opcodes.V21
                name = BOOTSTRAPPER_INTERNAL_NAME
                access = Opcodes.ACC_PUBLIC
                superName = "java/lang/Object"
                interfaces = listOf("io/papermc/paper/plugin/bootstrap/PluginBootstrap")
                methods = mutableListOf(
                    MethodNode(
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        "()V"
                    ) {
                        addLabel()
                        aLoad(0)
                        invokeSpecial("java/lang/Object", "<init>", "()V")
                        _return()
                    }
                )
            }
        } else {
            path.inputStream().use { ins ->
                val reader = ClassReader(ins)
                reader.accept(bootstrapper, 0)
            }
        }
        
        // inserts AddonBootstrapper.bootstrap(context, getClass().getClassLoader()) at beginning
        val bootstrap = bootstrapper.methods.firstOrNull { it.name == "bootstrap" }
            ?: defaultBootstrap.also { bootstrapper.methods.add(it) }
        bootstrap.instructions.insert(buildInsnList {
            addLabel()
            aLoad(1)
            aLoad(0)
            invokeVirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
            invokeVirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
            invokeStatic(
                "xyz/xenondevs/nova/addon/AddonBootstrapper",
                "bootstrap",
                "(Lio/papermc/paper/plugin/bootstrap/BootstrapContext;Ljava/lang/ClassLoader;)V"
            )
        })
        
        // inserts AddonBootstrapper.handleJavaPluginCreated(JavaPlugin, PluginProviderContext, ClassLoader) before return
        val createPlugin = bootstrapper.methods.firstOrNull { it.name == "createPlugin" }
            ?: defaultCreatePlugin.also { bootstrapper.methods.add(it) }
        createPlugin.insertBeforeEvery(buildInsnList {
            dup()
            aLoad(1) // PluginProviderContext
            aLoad(0)
            invokeVirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;")
            invokeVirtual("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;")
            invokeStatic(
                "xyz/xenondevs/nova/addon/AddonBootstrapper",
                "handleJavaPluginCreated",
                "(Lorg/bukkit/plugin/java/JavaPlugin;Lio/papermc/paper/plugin/bootstrap/PluginProviderContext;Ljava/lang/ClassLoader;)V"
            )
        }) { it.opcode == Opcodes.ARETURN }
        
        val bin = ClassWriter(ClassWriter.COMPUTE_FRAMES).also(bootstrapper::accept).toByteArray()
        path.parent.createDirectories()
        path.writeBytes(bin)
    }
    
    private fun generateLoader(path: Path) {
        val libraryLoaderConfiguration = project.configurations.getByName("libraryLoader")
        val dependencyCoordinates: List<String> = libraryLoaderConfiguration.incoming.dependencies.asSequence()
            .filterIsInstance<ExternalModuleDependency>()
            .map { dependency ->
                val artifact = dependency.artifacts.firstOrNull()
                if (artifact != null)
                    "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
                else "${dependency.group}:${dependency.name}:${dependency.version}"
            }
            .toList()
        
        val repositoryUrls: List<String>
        if (dependencyCoordinates.isNotEmpty()) {
            repositoryUrls = project.repositories.asSequence()
                .filterIsInstance<MavenArtifactRepository>()
                .filter { it !is DefaultMavenLocalArtifactRepository }
                .map { it.url.toString() }
                .toCollection(LinkedHashSet()) // remove duplicates, keep order
                .toList()
        } else {
            repositoryUrls = emptyList()
        }
        
        val loader = ClassNode(Opcodes.ASM9).apply {
            version = Opcodes.V21
            name = PLUGIN_LOADER_INTERNAL_NAME
            access = Opcodes.ACC_PUBLIC
            superName = "java/lang/Object"
            interfaces = listOf("io/papermc/paper/plugin/loader/PluginLoader")
            methods = listOf(
                // <init>
                MethodNode(
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V"
                ) {
                    addLabel()
                    aLoad(0)
                    invokeSpecial("java/lang/Object", "<init>", "()V")
                    _return()
                },
                
                // classLoader
                MethodNode(
                    Opcodes.ACC_PUBLIC,
                    "classloader",
                    "(Lio/papermc/paper/plugin/loader/PluginClasspathBuilder;)V"
                ) {
                    addLabel()
                    aLoad(1)
                    
                    // var resolver = new MavenLibraryResolver()
                    new("io/papermc/paper/plugin/loader/library/impl/MavenLibraryResolver")
                    dup()
                    invokeSpecial("io/papermc/paper/plugin/loader/library/impl/MavenLibraryResolver", "<init>", "()V")
                    
                    // resolver.addRepository(new RemoteRepository.Builder(repo.name, "default", repo.url).build())
                    for ((i, url) in repositoryUrls.withIndex()) {
                        dup()
                        new("org/eclipse/aether/repository/RemoteRepository\$Builder")
                        dup()
                        ldc("maven$i")
                        ldc("default")
                        ldc(url)
                        invokeSpecial("org/eclipse/aether/repository/RemoteRepository\$Builder", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V")
                        invokeVirtual("org/eclipse/aether/repository/RemoteRepository\$Builder", "build", "()Lorg/eclipse/aether/repository/RemoteRepository;")
                        invokeVirtual("io/papermc/paper/plugin/loader/library/impl/MavenLibraryResolver", "addRepository", "(Lorg/eclipse/aether/repository/RemoteRepository;)V")
                    }
                    
                    // resolver.addDependency(new Dependency(new DefaultArtifact(depCoord), null))
                    for (depCoord in dependencyCoordinates) {
                        dup()
                        new("org/eclipse/aether/graph/Dependency")
                        dup()
                        new("org/eclipse/aether/artifact/DefaultArtifact")
                        dup()
                        ldc(depCoord)
                        invokeSpecial("org/eclipse/aether/artifact/DefaultArtifact", "<init>", "(Ljava/lang/String;)V")
                        constNull()
                        invokeSpecial("org/eclipse/aether/graph/Dependency", "<init>", "(Lorg/eclipse/aether/artifact/Artifact;Ljava/lang/String;)V")
                        invokeVirtual("io/papermc/paper/plugin/loader/library/impl/MavenLibraryResolver", "addDependency", "(Lorg/eclipse/aether/graph/Dependency;)V")
                    }
                    
                    // pluginClasspathBuilder.addLibrary(resolver)
                    invokeInterface("io/papermc/paper/plugin/loader/PluginClasspathBuilder", "addLibrary", "(Lio/papermc/paper/plugin/loader/library/ClassPathLibrary;)Lio/papermc/paper/plugin/loader/PluginClasspathBuilder;")
                    
                    pop()
                    _return()
                }
            )
        }
        
        val bin = ClassWriter(ClassWriter.COMPUTE_FRAMES).also(loader::accept).toByteArray()
        path.parent.createDirectories()
        path.writeBytes(bin)
    }
    
}