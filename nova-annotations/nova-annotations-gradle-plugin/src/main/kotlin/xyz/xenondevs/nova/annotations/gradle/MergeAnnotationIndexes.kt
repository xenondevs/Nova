package xyz.xenondevs.nova.annotations.gradle

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import xyz.xenondevs.nova.annotations.AnnotationIndex
import java.util.zip.ZipFile

@CacheableTask
abstract class MergeAnnotationIndexes : DefaultTask() {
    
    @get:Classpath
    abstract val jars: ConfigurableFileCollection
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun merge() {
        val classes = sortedMapOf<String, AnnotationIndex.Class>()
        val seen = HashSet<String>()
        for (jar in jars) {
            ZipFile(jar).use { zip ->
                val indexedClasses = zip.getEntry(AnnotationIndex.FILE_NAME)?.let { entry ->
                    zip.getInputStream(entry).use { Json.decodeFromStream<AnnotationIndex>(it).classes }
                }.orEmpty()
                
                // Unindexed classes also take precedence over duplicates in later jars.
                for (entry in zip.entries()) {
                    if (!entry.name.endsWith(".class"))
                        continue
                    val name = entry.name.removeSuffix(".class")
                    if (seen.add(name))
                        indexedClasses[name]?.let { classes[name] = it }
                }
            }
        }
        outputFile.get().asFile.outputStream().use { Json.encodeToStream(AnnotationIndex(classes = classes), it) }
    }
    
}
