package xyz.xenondevs.novagradle.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

internal abstract class SyncInjectables @Inject constructor(
    objects: ObjectFactory
) : Sync() {
    
    @get:Input
    @get:Optional
    val bootstrapper: Property<String> = objects.property()
    
    @get:Input
    @get:Optional
    val loader: Property<String> = objects.property()
    
    @get:OutputDirectory
    val outputDir: DirectoryProperty = objects.directoryProperty()
    
    @get:OutputFile
    @get:Optional
    val bootstrapperFile: RegularFileProperty = objects.fileProperty()
        .convention(outputDir.file(bootstrapper))
    
    @get:OutputFile
    @get:Optional
    val loaderFile: RegularFileProperty = objects.fileProperty()
        .convention(outputDir.file(loader))
    
    init {
        eachFile {
            if (path != bootstrapper.orNull && path != loader.orNull) {
                exclude()
            }
        }
        into(outputDir)
    }
    
}