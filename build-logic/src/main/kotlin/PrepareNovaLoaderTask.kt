import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class PrepareNovaLoaderTask : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val libraries: ConfigurableFileCollection
    
    @get:Input
    abstract val libraryPaths: MapProperty<String, String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun run() {
        val outDir = outputDir.get().asFile
        val libPaths = libraryPaths.get().map { (source, path) ->
            copyToLibs(File(source), path, outDir)
        }
        outDir.resolve("nova-libraries").writeText(libPaths.joinToString("\n"))
    }
    
    private fun copyToLibs(file: File, path: String, out: File): String {
        val dst = out.resolve(path)
        dst.parentFile.mkdirs()
        file.copyTo(dst, true)
        
        return "/$path"
    }
    
}
