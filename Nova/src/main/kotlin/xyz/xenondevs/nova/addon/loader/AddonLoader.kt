package xyz.xenondevs.nova.addon.loader

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonDescription
import xyz.xenondevs.nova.addon.AddonLogger
import xyz.xenondevs.nova.addon.AddonManager
import java.io.File

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
        
        check(description.novaVersion <= NOVA.version) {
            "This addon is made for a newer version of Nova " +
                "(v${description.novaVersion}). This server is running Nova v${NOVA.version}"
        }
    }
    
    fun load(): Addon {
        val mainClass = classLoader.loadClass(description.main)
        addon = mainClass.constructors.first { it.parameterCount == 0 }.newInstance() as Addon
        addon.addonFile = file
        addon.description = description
        addon.logger = logger
        addon.dataFolder = File(AddonManager.addonsDir, description.id)
        return addon
    }
    
}