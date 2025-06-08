package xyz.xenondevs.novagradle.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import java.io.File

abstract class GenerateWailaTexturesExtension {
    
    abstract val resourcesDir: DirectoryProperty
    
    abstract val filter: Property<(File) -> Boolean>
    
}