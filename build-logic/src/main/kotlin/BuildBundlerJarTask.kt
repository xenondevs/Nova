import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

abstract class BuildBundlerJarTask : DefaultTask() {
    
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val novaInput: RegularFileProperty
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val input: ConfigurableFileCollection
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val libraries: ConfigurableFileCollection
    
    @get:Input
    abstract val libraryPaths: MapProperty<String, String>
    
    @get:OutputFile
    abstract val output: RegularFileProperty
    
    @TaskAction
    fun run() {
        val jar = output.get().asFile
        jar.parentFile.mkdirs()
        
        ZipOutputStream(jar.outputStream().buffered()).use { out ->
            val paths = HashSet<String>()
            include(out, listOf(novaInput.get().asFile), paths, includeMeta = true)
            include(out, input.files, paths, includeMeta = false)
            
            // include dependencies
            val libPaths = libraryPaths.get().map { (source, path) ->
                copyToLibs(out, File(source), path, paths)
            }
            
            out.putNextEntry(ZipEntry("nova-libraries"))
            out.write(libPaths.joinToString("\n").encodeToByteArray())
        }
    }
    
    private fun copyToLibs(out: ZipOutputStream, file: File, path: String, skip: Set<String>): String {
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
