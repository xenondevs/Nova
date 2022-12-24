package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonDescription
import xyz.xenondevs.nova.addon.AddonLogger
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.initialize.InitializationException
import java.io.File
import kotlin.reflect.jvm.jvmName

internal class AddonLoader(val file: File) {
    
    val classLoader = AddonClassLoader(this, javaClass.classLoader)
    val description: AddonDescription
    val logger: AddonLogger
    lateinit var addon: Addon
    
    init {
        val descriptionFile = classLoader.getResourceAsStream("addon.yml")
            ?: throw IllegalArgumentException("Could not find addon.yml")
        
        description = AddonDescription.deserialize(descriptionFile.reader())
        logger = AddonLogger(description.name)
        
        if (!IS_DEV_SERVER && description.novaVersion.compareTo(NOVA.version, 2) != 0)
            throw InitializationException("This addon is made for a different version of Nova (v${description.novaVersion})")
    }
    
    fun load(): Addon {
        val mainClass = classLoader.loadClass(description.main).kotlin
        
        val instance = mainClass.objectInstance ?: throw IllegalStateException("Main class is not a singleton object")
        addon = instance as? Addon ?: throw IllegalStateException("Main class is not a subclass of ${Addon::class.jvmName}")
        
        addon.addonFile = file
        addon.description = description
        addon.logger = logger
        addon.dataFolder = File(AddonManager.addonsDir, description.id)
        
        return addon
    }
    
}