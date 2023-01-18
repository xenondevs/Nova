import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import org.bukkit.configuration.file.YamlConfiguration
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import java.io.File
import java.io.InputStreamReader

private const val MAVEN_CENTRAL = "https://repo1.maven.org/maven2/"

abstract class BuildLoaderJarTask : DefaultTask() {
    
    private val mojangMapped = project.hasProperty("mojang-mapped") || System.getProperty("mojang-mapped") != null
    
    @get:Input
    lateinit var nova: Project
    
    @get:Input
    lateinit var novaAPI: Project
    
    @get:Input
    lateinit var novaLoader: Project
    
    @TaskAction
    fun run() {
        val novaFile = getOutputFile(nova)
        val novaAPIFile = getOutputFile(novaAPI)
        val novaLoaderFile = getOutputFile(novaLoader)
        
        // copy loader jar to out jar
        val outFile = File(project.buildDir, "Nova-${project.version}.jar")
        novaLoaderFile.copyTo(outFile, true)
        
        // include nova.jar
        val zip = ZipFile(outFile)
        zip.addFile(novaFile, ZipParameters().apply { fileNameInZip = "nova.jar" })
        
        // include api classes
        val apiZip = ZipFile(novaAPIFile)
        apiZip.fileHeaders.forEach {
            if (it.isDirectory)
                return@forEach
            
            zip.addStream(apiZip.getInputStream(it), ZipParameters().apply { fileNameInZip = it.fileName })
        }
        
        // generate libraries.yml
        zip.addStream(
            generateLibrariesYaml(nova).saveToString().byteInputStream(),
            ZipParameters().apply { fileNameInZip = "libraries.yml" }
        )
        
        // update plugin.yml
        val pluginYml = zip.getInputStream(zip.getFileHeader("plugin.yml"))
            .use { YamlConfiguration.loadConfiguration(InputStreamReader(it)) }
        setLibraries(pluginYml, "spigotLoader", false)
        zip.addStream(
            pluginYml.saveToString().byteInputStream(),
            ZipParameters().apply { fileNameInZip = "plugin.yml" }
        )
        
        // close zip files
        zip.close()
        apiZip.close()
        
        // copy to custom output directory
        val customOutDir = (project.findProperty("outDir") as? String)?.let(::File)
            ?: System.getProperty("outDir")?.let(::File)
            ?: return
        customOutDir.mkdirs()
        val copyTo = File(customOutDir, outFile.name)
        outFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    private fun generateLibrariesYaml(project: Project): YamlConfiguration {
        val librariesYml = YamlConfiguration()
        
        librariesYml["repositories"] = project.repositories.asSequence()
            .filterIsInstance<DefaultMavenArtifactRepository>()
            .filter { it !is DefaultMavenLocalArtifactRepository }
            .mapTo(HashSet()) { it.url.toString() }
            .apply { this -= MAVEN_CENTRAL }
            .toList() // Required for proper serialization
        
        setLibraries(librariesYml, "novaLoader", true)
        excludeConfiguration(librariesYml, "spigotRuntime")
        
        return librariesYml
    }
    
    @Suppress("SENSELESS_COMPARISON") // it isn't
    private fun setLibraries(cfg: YamlConfiguration, configuration: String, writeExclusions: Boolean) {
        cfg["libraries"] = project.configurations.getByName(configuration)
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
            exclusions += project.configurations.getByName(configuration).excludeRules.map { "${it.group}:${it.module}" }
            cfg["exclusions"] = exclusions
        }
    }
    
    private fun excludeConfiguration(cfg: YamlConfiguration, configuration: String) {
        val exclusions = cfg.getStringList("exclusions")
        
        exclusions += project.configurations.getByName(configuration)
            .incoming.artifacts.artifacts.asSequence()
            .map { it.variant.owner }
            .filterIsInstance<DefaultModuleComponentIdentifier>()
            .map { "${it.group}:${it.module}" }
        
        cfg["exclusions"] = exclusions
    }
    
    private fun getArtifactCoords(dependency: DefaultExternalModuleDependency): String {
        val artifact = dependency.artifacts.firstOrNull()?.takeUnless { it.classifier == "remapped-mojang" && !mojangMapped }
        return if (artifact != null)
            "${dependency.group}:${dependency.name}:${artifact.extension}:${artifact.classifier}:${dependency.version}"
        else "${dependency.group}:${dependency.name}:${dependency.version}"
    }
    
    private fun getOutputFile(project: Project): File {
        return getOutputFile(project.tasks.named<Jar>("jar").get())
    }
    
    private fun getOutputFile(jar: Jar): File {
        val dir = jar.destinationDirectory.get().asFile
        var name = listOf(
            jar.archiveBaseName.orNull ?: "",
            jar.archiveAppendix.orNull ?: "",
            jar.archiveVersion.orNull ?: "",
            jar.archiveClassifier.orNull ?: ""
        ).filterNot(String::isBlank).joinToString("-")
        jar.archiveExtension.orNull?.let { name += ".$it" }
        
        return File(dir, name)
    }
    
}