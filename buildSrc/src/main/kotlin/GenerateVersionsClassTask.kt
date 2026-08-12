import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateVersionsClassTask : DefaultTask() {
    
    @get:Input
    abstract val novaVersion: Property<String>
    
    @get:Input
    abstract val paperVersion: Property<String>
    
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    
    @TaskAction
    fun run() {
        val novaVersion = novaVersion.get()
        val paperVersion = paperVersion.get()
        val src = outputDirectory.file("xyz/xenondevs/novagradle/Versions.kt").get().asFile
        src.parentFile.mkdirs()
        src.writeText(
            """
            package xyz.xenondevs.novagradle

            internal object Versions {
                const val NOVA = "$novaVersion"
                const val NOVA_RELEASE = "${Regex("""^(\d+.\d+).*$""").matchEntire(novaVersion)!!.groupValues[1]}"
                const val PAPER = "$paperVersion"
                const val PAPER_API_VERSION = "${paperVersion.substring(0, 4)}"
            }
            """.trimIndent()
        )
    }
    
}
