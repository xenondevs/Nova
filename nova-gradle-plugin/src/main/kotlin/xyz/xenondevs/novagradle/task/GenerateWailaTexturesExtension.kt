package xyz.xenondevs.novagradle.task

import org.gradle.api.provider.Property
import java.io.File

abstract class GenerateWailaTexturesExtension {
    
    abstract val novaVersion: Property<String>
    
    abstract val addonId: Property<String>
    
    abstract val resourcesDir: Property<String>
    
    abstract val filter: Property<(File) -> Boolean>
    
}