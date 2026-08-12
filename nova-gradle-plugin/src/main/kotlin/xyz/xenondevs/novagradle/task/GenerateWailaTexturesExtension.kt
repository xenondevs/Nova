package xyz.xenondevs.novagradle.task

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class GenerateWailaTexturesExtension {
    
    abstract val resourcesDir: DirectoryProperty
    
    /**
     * A regular expression matched against generated WAILA texture file names.
     * Defaults to matching every file name.
     */
    abstract val filter: Property<String>
    
}
