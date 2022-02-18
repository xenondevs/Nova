package xyz.xenondevs.nova.addon.loader

import com.google.gson.JsonParser
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonDescription
import xyz.xenondevs.nova.addon.AddonLogger
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import java.io.File

class AddonLoader(val file: File) {
    
    val classLoader = AddonClassLoader(file, javaClass.classLoader)
    val description: AddonDescription
    lateinit var addon: Addon
    
    init {
        val descriptionFile = classLoader.getResourceAsStream("addon.json")
            ?: throw IllegalArgumentException("Could not find addon.json in $file")
        
        description = GSON.fromJson(JsonParser.parseReader(descriptionFile.reader()))
            ?: throw IllegalArgumentException("Could not deserialize addon description")
    }
    
    fun load(): Addon {
        val mainClass = classLoader.loadClass(description.main)
        addon = mainClass.constructors.first { it.parameterCount == 0 }.newInstance() as Addon
        addon.addonFile = file
        addon.description = description
        addon.dataFolder = File(NOVA.dataFolder, "addons/${description.id}/")
        addon.logger = AddonLogger(addon)
        return addon
    }
    
}