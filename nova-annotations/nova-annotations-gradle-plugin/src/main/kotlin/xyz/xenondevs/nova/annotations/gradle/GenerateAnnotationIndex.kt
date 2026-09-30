package xyz.xenondevs.nova.annotations.gradle

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import xyz.xenondevs.nova.annotations.AnnotationIndex
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString

@CacheableTask
abstract class GenerateAnnotationIndex : DefaultTask() {
    
    @get:Incremental
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val classes: ConfigurableFileCollection
    
    @get:Input
    abstract val classAnnotations: SetProperty<String>
    
    @get:Input
    abstract val methodAnnotations: SetProperty<String>
    
    @get:OutputFile
    abstract val outputFile: RegularFileProperty
    
    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun generate(changes: InputChanges) {
        val output = outputFile.get().asFile
        val entries = if (changes.isIncremental) {
            output.inputStream().use { Json.decodeFromStream<AnnotationIndex>(it).classes.toSortedMap() }
        } else {
            sortedMapOf()
        }
        
        val scanner = AnnotationScanner(classAnnotations.get(), methodAnnotations.get())
        for (change in changes.getFileChanges(classes)) {
            val path = Path(change.normalizedPath)
            if (change.fileType != FileType.FILE || path.extension != "class")
                continue
            
            val name = path.invariantSeparatorsPathString.removeSuffix(".class")
            entries.remove(name)
            if (change.changeType == ChangeType.REMOVED)
                continue
            
            val [className, indexedClass] = scanner.scan(change.file.readBytes())
            require(className == name) { "Class $className does not match its path $name" }
            if (indexedClass.annotations.isNotEmpty() || indexedClass.methods.isNotEmpty())
                entries[name] = indexedClass
        }
        
        output.outputStream().buffered().use { Json.encodeToStream(AnnotationIndex(entries), it) }
    }
    
}
