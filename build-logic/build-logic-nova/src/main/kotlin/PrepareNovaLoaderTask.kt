import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault
abstract class PrepareNovaLoaderTask : DefaultTask() {
    
    @get:Input
    abstract val libraryPaths: ListProperty<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @TaskAction
    fun run() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(libraryPaths.get().joinToString("\n") { "/$it" })
    }
    
}
