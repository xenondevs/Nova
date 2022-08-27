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
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption

private const val MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/"

abstract class BuildLoaderJarTask : DefaultTask() {
    
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
        
        // close zip files
        zip.close()
        apiZip.close()
        
        // copy to custom output directory
        val customOutDir = System.getProperty("outDir")?.let(::File) ?: return
        customOutDir.mkdirs()
        val copyTo = File(customOutDir, outFile.name)
        outFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    @Suppress("SENSELESS_COMPARISON") // it isn't
    private fun generateLibrariesYaml(project: Project): YamlConfiguration {
        val cfg = YamlConfiguration()
        
        cfg["repositories"] = project.repositories.asSequence()
            .filterIsInstance<DefaultMavenArtifactRepository>()
            .filter { it !is DefaultMavenLocalArtifactRepository }
            .mapTo(HashSet()) { it.url.toString() }
            .apply { this -= MAVEN_CENTRAL }
            .toList() // Required for proper serialization
        
        val mojangMapped = System.getProperty("mojang-mapped") != null
        cfg["libraries"] = project.configurations.getByName("runtimeClasspath")
            .incoming.dependencies.asSequence()
            .filterIsInstance<DefaultExternalModuleDependency>()
            .mapTo(ArrayList()) { dep ->
                val artifact = dep.artifacts.firstOrNull()?.takeUnless { it.classifier == "remapped-mojang" && !mojangMapped }
                val coords = if (artifact != null)
                    "${dep.group}:${dep.name}:${artifact.extension}:${artifact.classifier}:${dep.version}"
                else "${dep.group}:${dep.name}:${dep.version}"
                
                val excludeRules = dep.excludeRules
                if (excludeRules.isNotEmpty()) {
                    val exCfg = YamlConfiguration()
                    exCfg["library"] = coords
                    exCfg["exclusions"] = excludeRules.map {
                        require(it.module != null) { "Exclusion rules need to specify a module" }
                        if (it.group != null)
                            "${it.group}:${it.module}" 
                        else it.module
                    }
                    
                    return@mapTo exCfg
                }
                
                return@mapTo coords
            }
        
        return cfg
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