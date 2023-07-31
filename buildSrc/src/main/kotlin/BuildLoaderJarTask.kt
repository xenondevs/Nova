
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.bukkit.configuration.file.YamlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import xyz.xenondevs.stringremapper.FileRemapper
import xyz.xenondevs.stringremapper.Mappings
import xyz.xenondevs.stringremapper.RemapGoal
import java.io.File
import java.nio.file.FileSystems
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.appendText
import kotlin.io.path.writeText

private const val MAVEN_CENTRAL = "https://repo1.maven.org/maven2/"

abstract class BuildLoaderJarTask : DefaultTask() {
    
    @get:Input
    abstract var nova: Project
    
    @get:Input
    abstract var novaApi: Project
    
    @get:Input
    abstract var hooks: List<Project>
    
    @get:Input
    abstract val gameVersion: Property<String>
    
    @get:Input
    abstract var remap: Boolean
    
    private val buildDir = project.layout.buildDirectory.asFile.get()
    
    @TaskAction
    fun run() {
        val novaFile = createNovaJar()
        writeLibraries(novaFile)
        
        // copy to custom output directory
        val customOutDir = (project.findProperty("outDir") as? String)?.let(::File)
            ?: System.getProperty("outDir")?.let(::File)
            ?: return
        customOutDir.mkdirs()
        val copyTo = File(customOutDir, novaFile.name)
        novaFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    private fun createNovaJar(): File {
        buildDir.mkdirs()
        
        val mapsMojang = buildDir.resolve("maps-mojang.txt")
        val mapsSpigot = buildDir.resolve("maps-spigot.csrg")
        val mappings = Mappings.loadOrDownload(gameVersion.get(), mapsMojang, mapsSpigot, buildDir.resolve("mappings.json"))
        val remapper = FileRemapper(mappings, if (remap) RemapGoal.SPIGOT else RemapGoal.MOJANG)
        
        // create jar
        val novaFile = buildDir.resolve("Nova-${project.version}.jar")
        buildJarFromProjects(novaFile, hooks + novaApi + nova) { remapper.remap(it.inputStream()) ?: it }
        if (remap) {
            // remap nova jar with specialsource
            val obfFile = novaFile.parentFile.resolve(novaFile.nameWithoutExtension + "-obf.jar")
            remapSpigot(novaFile, obfFile, mapsMojang, true) // mojang -> obf
            remapSpigot(obfFile, novaFile, mapsSpigot, false) // obf -> spigot
        }
        
        return novaFile
    }
    
    private fun buildJarFromProjects(file: File, projects: List<Project>, remapper: (ByteArray) -> ByteArray = { it }) {
        ZipOutputStream(file.outputStream()).use { out ->
            projects.forEach { project ->
                iterateClasses(project) { file, path ->
                    out.putNextEntry(ZipEntry(path))
                    val bin = remapper(file.readBytes())
                    out.write(bin)
                }
                iterateResources(project) { file, path ->
                    out.putNextEntry(ZipEntry(path))
                    file.inputStream().use { inp -> inp.transferTo(out) }
                }
            }
        }
    }
    
    private fun iterateClasses(project: Project, run: (File, String) -> Unit) {
        val kotlinClasses = project.layout.buildDirectory.asFile.get().resolve("classes/kotlin/main")
        val javaClasses = project.layout.buildDirectory.asFile.get().resolve("classes/java/main")
        iterateClasses(kotlinClasses,  run)
        iterateClasses(javaClasses,  run)
    }
    
    private fun iterateClasses(dir: File, run: (File, String) -> Unit) {
        dir.walkTopDown()
            .filter { it.isFile }
            .map { it to it.relativeTo(dir).invariantSeparatorsPath }
            .filter { (_, path) -> !path.startsWith("META-INF") }
            .forEach { (file, path) -> run(file, path) }
    }
    
    private fun iterateResources(project: Project, run: (File, String) -> Unit) {
        val resources = project.layout.buildDirectory.asFile.get().resolve("resources/main")
        resources.walkTopDown()
            .filter { it.isFile }
            .forEach {
                val path = it.relativeTo(resources).invariantSeparatorsPath
                run(it, path)
            }
    }
    
    private fun remapSpigot(inFile: File, outFile: File, srgIn: File, reverse: Boolean) {
        val mapping = JarMapping()
        val inheritanceProviders = JointProvider().also(mapping::setFallbackInheritanceProvider)
        val jars = ArrayList<Jar>()
        
        // load mapping file
        mapping.loadMappings(srgIn.absolutePath, reverse, false, null, null)
        
        // inheritance provider
        val inJar = Jar.init(inFile).also(jars::add)
        inheritanceProviders.add(JarProvider(inJar))
        
        // load all project dependencies as inheritance providers
        nova.configurations.getByName("compileClasspath").incoming.artifacts.artifactFiles.files.forEach {
            val jar = Jar.init(it).also(jars::add)
            inheritanceProviders.add(JarProvider(jar))
        }
        
        // remap jar
        val jarMap = JarRemapper(null, mapping, null)
        jarMap.remapJar(inJar, outFile)
        
        // close file handles to inputs
        jars.forEach(Jar::close)
    }
    
    private fun writeLibraries(bundlerFile: File) {
        FileSystems.newFileSystem(bundlerFile.toPath()).use {
            val bundlerZipRoot = it.rootDirectories.first()
            
            // generate libraries.yml
            bundlerZipRoot.resolve("libraries.yml").writeText(generateNovaLoaderLibrariesYaml().saveToString())
            // add spigot loader libraries to plugin.yml
            bundlerZipRoot.resolve("paper-plugin.yml").appendText("\n" + generateSpigotLoaderLibrariesYaml().saveToString())
        }
    }
    
    private fun generateNovaLoaderLibrariesYaml(): YamlConfiguration {
        val librariesYml = YamlConfiguration()
        
        librariesYml["repositories"] = nova.repositories.asSequence()
            .filterIsInstance<MavenArtifactRepository>()
            .filter { it !is DefaultMavenLocalArtifactRepository }
            .mapTo(HashSet()) { it.url.toString() }
            .apply { this -= MAVEN_CENTRAL }
            .toList() // Required for proper serialization
        
        setLibraries(librariesYml, "novaLoader", true)
        excludeConfiguration(librariesYml, "spigotRuntime")
        
        return librariesYml
    }
    
    private fun generateSpigotLoaderLibrariesYaml(): YamlConfiguration {
        val cfg = YamlConfiguration()
        setLibraries(cfg, "spigotLoader", false)
        return cfg
    }
    
    @Suppress("SENSELESS_COMPARISON") // it isn't
    private fun setLibraries(cfg: YamlConfiguration, configuration: String, writeExclusions: Boolean) {
        cfg["libraries"] = nova.configurations.getByName(configuration)
            .incoming.dependencies.asSequence()
            .filterIsInstance<ExternalModuleDependency>()
            .mapTo(ArrayList()) { dep ->
                val coords = getArtifactCoords(dep)
                val excludeRules = dep.excludeRules
                if (excludeRules.isNotEmpty()) {
                    val exCfg = YamlConfiguration()
                    exCfg["library"] = coords
                    exCfg["exclusions"] = excludeRules.map {
                        require(it.group != null && it.module != null) { "Exclusion rules need to specify group and module" }
                        "${it.group}:${it.module}::jar"
                    }
                    
                    return@mapTo exCfg
                }
                
                return@mapTo coords
            }
        
        if (writeExclusions) {
            val exclusions = cfg.getStringList("exclusions")
            exclusions += nova.configurations.getByName(configuration).excludeRules.map { "${it.group}:${it.module}" }
            cfg["exclusions"] = exclusions
        }
    }
    
    private fun excludeConfiguration(cfg: YamlConfiguration, configuration: String) {
        val exclusions = cfg.getStringList("exclusions")
        
        exclusions += nova.configurations.getByName(configuration)
            .incoming.artifacts.artifacts.asSequence()
            .map { it.variant.owner }
            .filterIsInstance<DefaultModuleComponentIdentifier>()
            .map { "${it.group}:${it.module}" }
        
        cfg["exclusions"] = exclusions
    }
    
    private fun getArtifactCoords(dependency: ExternalModuleDependency): String {
        val artifact = dependency.artifacts.firstOrNull()?.takeUnless { it.classifier == "remapped-mojang" && remap }
        return if (artifact != null)
            "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
        else "${dependency.group}:${dependency.name}:${dependency.version}"
    }
    
}