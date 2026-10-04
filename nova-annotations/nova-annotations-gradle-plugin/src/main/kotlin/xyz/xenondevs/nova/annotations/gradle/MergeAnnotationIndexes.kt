package xyz.xenondevs.nova.annotations.gradle

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import xyz.xenondevs.nova.annotations.AnnotationIndex

@CacheableTask
abstract class MergeAnnotationIndexes : DefaultTask() {
    
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val indexes: ConfigurableFileCollection
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun merge() {
        val classes = sortedMapOf<String, AnnotationIndex.Class>()
        for (index in indexes) {
            classes.putAll(index.inputStream().buffered()
                .use { Json.decodeFromStream<AnnotationIndex>(it).classes })
        }
        outputFile.get().asFile.outputStream().buffered()
            .use { Json.encodeToStream(AnnotationIndex(classes = classes), it) }
    }
    
}
