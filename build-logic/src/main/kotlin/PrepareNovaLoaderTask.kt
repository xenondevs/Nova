
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault
abstract class PrepareNovaLoaderTask : DefaultTask() {
    
    @get:Classpath
    abstract val exclude: Property<Configuration>
    
    @get:Classpath
    abstract val include: Property<Configuration>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @TaskAction
    fun run() {
        val outDir = outputDir.get().asFile
        includeLibraries(outDir)
    }
    
    private fun includeLibraries(out: File) {
        val runtimeArtifacts = exclude.get().incoming.artifacts.artifacts
            .mapNotNullTo(HashSet()) { (it.id.componentIdentifier as? ModuleComponentIdentifier)?.moduleIdentifier }
        val libPaths = include.get().incoming.artifacts.artifacts
            .filter { (it.id.componentIdentifier as ModuleComponentIdentifier).moduleIdentifier !in runtimeArtifacts }
            .mapNotNull { copyToLibs(it, out) }
        
        out.resolve("nova-libraries").writeText(libPaths.joinToString("\n"))
    }
    
    private fun copyToLibs(artifact: ResolvedArtifactResult, out: File): String? {
        val file = artifact.file
        val id = artifact.id.componentIdentifier as? ModuleComponentIdentifier
            ?: return null
        
        val path = "lib" +
            "/" + id.group.replace('.', '/') +
            "/" + id.module +
            "/" + id.version +
            "/" + file.name
        
        val dst = out.resolve(path)
        dst.parentFile.mkdirs()
        file.copyTo(dst, true)
        
        return "/$path"
    }
    
}