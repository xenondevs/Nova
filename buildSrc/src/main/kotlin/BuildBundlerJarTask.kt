import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

abstract class BuildBundlerJarTask : DefaultTask() {
    
    @get:InputFile
    abstract val novaInput: RegularFileProperty
    
    @get:InputFiles
    abstract val input: ConfigurableFileCollection
    
    @get:OutputFile
    abstract val output: RegularFileProperty
    
    private val buildDir: File
        get() = project.layout.buildDirectory.asFile.get()
    
    @TaskAction
    fun run() {
        val novaFile = createLoaderJar()
        
        // copy to specified destination
        val copyTo = output.get().asFile
        if (copyTo == novaFile)
            return
        copyTo.parentFile.mkdirs()
        novaFile.inputStream().use { ins -> copyTo.outputStream().use { out -> ins.copyTo(out) } }
    }
    
    private fun createLoaderJar(): File {
        buildDir.mkdirs()
        
        val jar = buildDir.resolve("Nova-${project.version}.jar")
        ZipOutputStream(jar.outputStream().buffered()).use { out ->
            val paths = HashSet<String>()
            include(out, listOf(novaInput.get().asFile), paths, includeMeta = true)
            include(out, input.files, paths, includeMeta = false)
            
            // include dependencies
            val runtimeArtifacts = project.configurations.getByName("paperweightDevelopmentBundleCompileClasspath").incoming.artifacts.artifacts
                .mapNotNullTo(HashSet()) { (it.id.componentIdentifier as? ModuleComponentIdentifier)?.moduleIdentifier }
            val libPaths = project.configurations.getByName("novaLoader").incoming.artifacts.artifacts
                .filter { (it.id.componentIdentifier as ModuleComponentIdentifier).moduleIdentifier !in runtimeArtifacts }
                .mapNotNull { copyToLibs(out, it, paths) }
            
            out.putNextEntry(ZipEntry("nova-libraries"))
            out.write(libPaths.joinToString("\n").encodeToByteArray())
        }
        
        return jar
    }
    
    private fun copyToLibs(out: ZipOutputStream, artifact: ResolvedArtifactResult, skip: Set<String>): String? {
        val file = artifact.file
        val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier
            ?: return null
        
        val path = "lib/" +
            id.group.replace('.', '/') +
            "/" + id.module +
            "/" + id.version +
            "/" + file.name
        
        if (path in skip)
            return path
        
        out.putNextEntry(ZipEntry(path))
        file.inputStream().use { inp -> inp.transferTo(out) }
        return path
    }
    
    private fun include(out: ZipOutputStream, jars: Iterable<File>, paths: MutableSet<String>, includeMeta: Boolean) {
        jars.forEach { jar ->
            ZipInputStream(jar.inputStream().buffered()).use { inp ->
                generateSequence { inp.nextEntry }
                    .filter { includeMeta || !it.name.startsWith("META-INF") }
                    .filter { !it.isDirectory && !paths.contains(it.name) }
                    .forEach { entry ->
                        out.putNextEntry(entry)
                        inp.transferTo(out)
                        paths.add(entry.name)
                    }
            }
        }
    }
    
}