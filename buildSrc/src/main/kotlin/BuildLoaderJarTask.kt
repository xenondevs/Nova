
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.bukkit.configuration.file.YamlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.kotlin.dsl.named
import xyz.xenondevs.stringremapper.FileRemapper
import xyz.xenondevs.stringremapper.Mappings
import xyz.xenondevs.stringremapper.RemapGoal
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.appendText
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText
import org.gradle.jvm.tasks.Jar as JarTask


private const val MAVEN_CENTRAL = "https://repo1.maven.org/maven2/"

abstract class BuildLoaderJarTask : DefaultTask() {
    
    @get:Input
    abstract var nova: Project
    
    @get:Input
    abstract var novaApi: Project
    
    @get:Input
    abstract var novaLoader: Project
    
    @get:Input
    abstract var hooks: List<Project>
    
    @get:Input
    abstract val spigotVersion: Property<String>
    
    @get:Input
    abstract var remap: Boolean
    
    @TaskAction
    fun run() {
        val bundlerFile = createBundlerJar()
        writeLibraries(bundlerFile)
        
        // copy to custom output directory
        val customOutDir = (project.findProperty("outDir") as? String)?.let(::File)
            ?: System.getProperty("outDir")?.let(::File)
            ?: return
        customOutDir.mkdirs()
        val copyTo = File(customOutDir, bundlerFile.name)
        bundlerFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    // TODO: use ZipFileSystem and JIMFS when on Kotlin 1.8.20
    private fun createBundlerJar(): Path {
        project.buildDir.mkdirs()
        
        val (mapsMojang, mapsSpigot) = resolveMappings(spigotVersion.get())
        val mappingsCache = project.buildDir.resolve("mappings.json")
        val mappings: Mappings
        if (mappingsCache.exists()) {
            mappings = Mappings.loadFromJson(mappingsCache)
        } else {
            mappings = Mappings.load(mapsMojang, mapsSpigot)
            mappings.writeToJson(mappingsCache)
        }
        val remapper = FileRemapper(mappings, if (remap) RemapGoal.SPIGOT else RemapGoal.MOJANG)
        
        // create nova jar
        val novaFile = project.buildDir.resolve("nova-remapped.jar").toPath()
        buildJarFromProjects(novaFile.toFile(), hooks + nova) { remapper.remap(it.inputStream()) ?: it }
        if (remap) {
            // remap nova jar with specialsource
            val obfFile = novaFile.parent.resolve(novaFile.nameWithoutExtension + "-obf.jar")
            remapSpigot(novaFile, obfFile, mapsMojang, true) // mojang -> obf
            remapSpigot(obfFile, novaFile, mapsSpigot, false) // obf -> spigot
        }
        
        // create bundler jar
        val bundlerFile = File(project.buildDir, "Nova-${project.version}.jar").toPath()
        buildJarFromProjects(bundlerFile.toFile(), listOf(novaLoader, novaApi))
        ZipFile(bundlerFile.toFile()).use { bundlerZip ->
            // include nova jar in bundler jar
            bundlerZip.addFile(novaFile.toFile(), ZipParameters().apply { fileNameInZip = "nova.jar" })
        }
        
        return bundlerFile
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
        val kotlinClasses = project.buildDir.resolve("classes/kotlin/main")
        val javaClasses = project.buildDir.resolve("classes/java/main")
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
        val resources = project.buildDir.resolve("resources/main")
        resources.walkTopDown()
            .filter { it.isFile }
            .forEach {
                val path = it.relativeTo(resources).invariantSeparatorsPath
                run(it, path)
            }
    }
    
    private fun remapSpigot(inFile: Path, outFile: Path, srgIn: Path, reverse: Boolean) {
        val mapping = JarMapping()
        val inheritanceProviders = JointProvider().also(mapping::setFallbackInheritanceProvider)
        
        // load mapping file
        mapping.loadMappings(srgIn.toFile().absolutePath, reverse, false, null, null)
        
        // inheritance provider
        val inJar = Jar.init(inFile.toFile())
        inheritanceProviders.add(JarProvider(inJar))
        
        // load all project dependencies as inheritance providers
        nova.configurations.getByName("compileClasspath").incoming.artifacts.artifactFiles.files.forEach {
            inheritanceProviders.add(JarProvider(Jar.init(it)))
        }
        
        // remap jar
        val jarMap = JarRemapper(null, mapping, null)
        jarMap.remapJar(inJar, outFile.toFile())
    }
    
    private fun resolveMappings(version: String): Pair<Path, Path> {
        val mojangMappings = project.dependencies.create("org.spigotmc:minecraft-server:$version:maps-mojang@txt").getFile(project)
        val spigotMappings = project.dependencies.create("org.spigotmc:minecraft-server:$version:maps-spigot@csrg").getFile(project)
        return Pair(mojangMappings, spigotMappings)
    }
    
    private fun Dependency.getFile(project: Project): Path =
        project.configurations.detachedConfiguration(this).singleFile.toPath()
    
    private fun writeLibraries(bundlerFile: Path) {
        FileSystems.newFileSystem(bundlerFile).use {
            val bundlerZipRoot = it.rootDirectories.first()
            
            // generate libraries.yml
            bundlerZipRoot.resolve("libraries.yml").writeText(generateNovaLoaderLibrariesYaml().saveToString())
            // add spigot loader libraries to plugin.yml
            bundlerZipRoot.resolve("plugin.yml").appendText("\n" + generateSpigotLoaderLibrariesYaml().saveToString())
        }
    }
    
    private fun generateNovaLoaderLibrariesYaml(): YamlConfiguration {
        val librariesYml = YamlConfiguration()
        
        librariesYml["repositories"] = nova.repositories.asSequence()
            .filterIsInstance<DefaultMavenArtifactRepository>()
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
            .filterIsInstance<DefaultExternalModuleDependency>()
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
    
    private fun getArtifactCoords(dependency: DefaultExternalModuleDependency): String {
        val artifact = dependency.artifacts.firstOrNull()?.takeUnless { it.classifier == "remapped-mojang" && remap }
        return if (artifact != null)
            "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
        else "${dependency.group}:${dependency.name}:${dependency.version}"
    }
    
    private fun getOutputFile(project: Project): Path {
        return getOutputFile(project.tasks.named<JarTask>("jar").get())
    }
    
    private fun getOutputFile(jar: JarTask): Path {
        val dir = jar.destinationDirectory.get().asFile
        var name = listOf(
            jar.archiveBaseName.orNull ?: "",
            jar.archiveAppendix.orNull ?: "",
            jar.archiveVersion.orNull ?: "",
            jar.archiveClassifier.orNull ?: ""
        ).filterNot(String::isBlank).joinToString("-")
        jar.archiveExtension.orNull?.let { name += ".$it" }
        
        return File(dir, name).toPath()
    }
    
}