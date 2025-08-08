import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@DisableCachingByDefault
abstract class BuildBundlerJarTask : DefaultTask() {
    
    @get:InputFile
    abstract val novaInput: RegularFileProperty
    
    @get:InputFiles
    abstract var input: FileCollection
    
    @get:OutputFile
    abstract val output: RegularFileProperty
    
    private val nova: Project
        get() = project.project(":nova")
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
        ZipOutputStream(jar.outputStream()).use { out ->
            include(out, listOf(novaInput.get().asFile), includeMeta = true)
            include(out, input.files)
            
            // include dependencies
//            val runtimeArtifacts = nova.configurations.getByName("mojangMappedServerRuntime").incoming.artifacts.artifacts
//                .mapNotNullTo(HashSet()) { (it.id.componentIdentifier as? ModuleComponentIdentifier)?.moduleIdentifier }
            nova.configurations.getByName("novaLoader").incoming.artifacts.artifacts
                .asSequence()
//                .filter { (it.id.componentIdentifier as ModuleComponentIdentifier).moduleIdentifier !in runtimeArtifacts }
                .forEach { artifact ->
                    val file = artifact.file
                    val id = artifact.id.componentIdentifier as ModuleComponentIdentifier
                    val path = "lib/" + id.group.replace('.', '/') + "/" + id.module + "/" + id.version + "/" + file.name
                    out.putNextEntry(ZipEntry(path))
                    file.inputStream().use { inp -> inp.transferTo(out) }
                }
        }
        
        return jar
    }
    
    private fun include(out: ZipOutputStream, jars: Iterable<File>, includeMeta: Boolean = false) {
        jars.forEach { jar ->
            ZipInputStream(jar.inputStream()).use { inp ->
                generateSequence { inp.nextEntry }
                    .filter { includeMeta || !it.name.startsWith("META-INF") }
                    .filter { !it.isDirectory }
                    .forEach { entry ->
                        out.putNextEntry(entry)
                        inp.transferTo(out)
                    }
            }
        }
    }
    
}