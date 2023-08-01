
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.md_5.specialsource.Jar
import net.md_5.specialsource.JarMapping
import net.md_5.specialsource.JarRemapper
import net.md_5.specialsource.provider.JarProvider
import net.md_5.specialsource.provider.JointProvider
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.stringremapper.FileRemapper
import xyz.xenondevs.stringremapper.Mappings
import xyz.xenondevs.stringremapper.RemapGoal
import java.io.File
import java.nio.file.FileSystems
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.copyTo

abstract class BuildLoaderJarTask : DefaultTask() {
    
    @get:Input
    abstract var novaLoader: Project
    
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
        val novaFile = createLoaderJar()
        
        // copy to custom output directory
        val customOutDir = (project.findProperty("outDir") as? String)?.let(::File)
            ?: System.getProperty("outDir")?.let(::File)
            ?: return
        customOutDir.mkdirs()
        val copyTo = File(customOutDir, novaFile.name)
        novaFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    private fun createLoaderJar(): File {
        buildDir.mkdirs()
        
        val mapsMojang = buildDir.resolve("maps-mojang.txt")
        val mapsSpigot = buildDir.resolve("maps-spigot.csrg")
        val mappings = Mappings.loadOrDownload(gameVersion.get(), mapsMojang, mapsSpigot, buildDir.resolve("mappings.json"))
        val remapper = FileRemapper(mappings, if (remap) RemapGoal.SPIGOT else RemapGoal.MOJANG)
        
        // create bundled jar
        val bundledFile = buildDir.resolve("Nova-${project.version}-bundled.jar")
        buildJarFromProjects(bundledFile, hooks + nova) { remapper.remap(it.inputStream()) ?: it }
        if (remap) {
            // remap nova jar with specialsource
            val obfFile = bundledFile.parentFile.resolve(bundledFile.nameWithoutExtension + "-obf.jar")
            remapSpigot(bundledFile, obfFile, mapsMojang, true) // mojang -> obf
            remapSpigot(obfFile, bundledFile, mapsSpigot, false) // obf -> spigot
        }
        
        // create loader jar
        val bundlerFile = buildDir.resolve("Nova-${project.version}.jar")
        buildJarFromProjects(bundlerFile, listOf(novaLoader, novaApi))
        
        FileSystems.newFileSystem(bundlerFile.toPath()).use {
            val bundlerZipRoot = it.rootDirectories.first()
            
            // bundled nova (nova.jar)
            bundledFile.toPath().copyTo(bundlerZipRoot.resolve("nova.jar"))
            
            // default libraries (libraries.json)
            createLibrariesJson(nova, "novaLoader", listOf("spigotRuntime", "paperweightDevelopmentBundle"))
                .writeToFile(bundlerZipRoot.resolve("libraries.json"))
            
            // prioritized libraries (queried before parent class loader) (prioritized_libraries.json)
            createLibrariesJson(nova, "prioritizedNovaLoader", listOf("novaLoader", "spigotRuntime", "paperweightDevelopmentBundle"))
                .writeToFile(bundlerZipRoot.resolve("prioritized_libraries.json"))
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
        val kotlinClasses = project.layout.buildDirectory.asFile.get().resolve("classes/kotlin/main")
        val javaClasses = project.layout.buildDirectory.asFile.get().resolve("classes/java/main")
        iterateClasses(kotlinClasses, run)
        iterateClasses(javaClasses, run)
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
    
    @Suppress("SENSELESS_COMPARISON") // falsely interpreted as non-nullable
    private fun createLibrariesJson(project: Project, configurationName: String, excludedConfigurationNames: List<String>): JsonObject {
        val json = JsonObject()
        
        // repositories
        val repositories = JsonArray().also { json.add("repositories", it) }
        project.repositories.asSequence()
            .filterIsInstance<MavenArtifactRepository>()
            .filter { it !is DefaultMavenLocalArtifactRepository }
            .mapTo(LinkedHashSet()) { it.url.toString() }
            .forEach(repositories::add)
        
        // libraries and exclusions
        val libraries = JsonArray().also { json.add("libraries", it) }
        val configuration = project.configurations.getByName(configurationName)
        
        fun writeExcludeRules(rules: Set<ExcludeRule>, excludes: JsonArray) {
            rules.forEach {
                require(it.group != null && it.module != null) { "Exclusion rules need to specify group and module" }
                excludes.add("${it.group}:${it.module}")
            }
        }
        
        configuration.incoming.dependencies.asSequence()
            .filterIsInstance<ExternalModuleDependency>()
            .forEach { dependency ->
                val library = JsonObject().also(libraries::add)
                
                library.addProperty("coords", getArtifactCoords(dependency))
                val excludeRules = dependency.excludeRules
                writeExcludeRules(excludeRules, JsonArray().also { library.add("excludes", it) })
            }
        
        val excludes = JsonArray().also { json.add("excludes", it) }
        writeExcludeRules(configuration.excludeRules, excludes)
        for (exclusionConfigurationName in excludedConfigurationNames) {
            project.configurations.getByName(exclusionConfigurationName)
                .incoming.artifacts.artifacts.asSequence()
                .map { it.variant.owner }
                .filterIsInstance<DefaultModuleComponentIdentifier>()
                .forEach { excludes.add("${it.group}:${it.module}") }
        }
        
        return json
    }
    
    private fun getArtifactCoords(dependency: ExternalModuleDependency): String {
        val artifact = dependency.artifacts.firstOrNull()?.takeUnless { it.classifier == "remapped-mojang" && remap }
        return if (artifact != null)
            "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
        else "${dependency.group}:${dependency.name}:${dependency.version}"
    }
    
}