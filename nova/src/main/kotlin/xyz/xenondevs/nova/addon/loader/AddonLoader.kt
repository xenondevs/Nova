package xyz.xenondevs.nova.addon.loader

import com.google.gson.JsonObject
import org.eclipse.aether.graph.Dependency
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.AddonDescription
import xyz.xenondevs.nova.addon.AddonLogger
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.initialize.InitializationException
import xyz.xenondevs.nova.addon.library.LibraryFileParser
import xyz.xenondevs.nova.util.data.useZip
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.reflect.jvm.jvmName

internal class AddonLoader(val file: File) {
    
    // init
    lateinit var logger: AddonLogger private set
    lateinit var description: AddonDescription private set
    lateinit var repositories: List<String> private set
    lateinit var libraries: List<Dependency> private set
    lateinit var exclusions: Set<String> private set
    
    // load
    lateinit var addon: Addon private set
    
    init {
        file.useZip { zip ->
            val metadataFile = zip.resolve("addon_metadata.json").takeIf(Path::exists)
                ?: throw throw IllegalArgumentException("Missing addon_metadata.json")
            val librariesFile = zip.resolve("addon_libraries.json")
                ?: throw IllegalArgumentException("Missing addon_libraries.json")
            
            description = AddonDescription.fromJson(metadataFile.parseJson() as JsonObject)
            logger = AddonLogger(description.name)
            
            if (!IS_DEV_SERVER && description.novaVersion.compareTo(NOVA.version, 2) != 0)
                throw InitializationException("This addon is made for a different version of Nova (v${description.novaVersion})")
            
            val librariesJson = librariesFile.parseJson() as? JsonObject
            if (librariesJson != null) {
                repositories = LibraryFileParser.readRepositories(librariesJson)
                libraries = LibraryFileParser.readLibraries(librariesJson)
                exclusions = LibraryFileParser.readExclusions(librariesJson)
            } else {
                repositories = emptyList()
                libraries = emptyList()
                exclusions = emptySet()
            }
        }
    }
    
    fun load(classLoader: ClassLoader): Addon {
        val mainClass = classLoader.loadClass(description.main).kotlin
        
        val instance = mainClass.objectInstance
            ?: throw IllegalStateException("Main class is not a singleton object")
        addon = instance as? Addon
            ?: throw IllegalStateException("Main class is not a subclass of ${Addon::class.jvmName}")
        
        addon.addonFile = file
        addon.description = description
        addon.logger = logger
        addon.dataFolder = File(AddonManager.addonsDir, description.id)
        
        return addon
    }
    
}