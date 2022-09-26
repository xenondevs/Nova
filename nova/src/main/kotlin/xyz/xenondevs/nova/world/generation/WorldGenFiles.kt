package xyz.xenondevs.nova.world.generation

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.getResources
import java.io.File

internal object WorldGenFiles {
    
    private val WORLD_GEN_FILE_PATTERN = Regex("""^[a-z][a-z\d_]*.json$""")
    
    private val WORLD_GEN_DIR = File(NOVA.dataFolder, ".data/worldgen")
    
    fun extract() {
        val existingPaths = ObjectArrayList<String>()
    
        // Extract world gen files
        existingPaths += getResources("data/worldgen/").mapNotNull(::extractWorldGenFile)
        
        // Extract world gen files from addons
        AddonManager.loaders.values.forEach { loader ->
            existingPaths += getResources(loader.file, "data/worldgen/").mapNotNull { extractWorldGenFile(it, loader) }
        }
        
        // find unedited world gen files that are no longer default and remove them
        WORLD_GEN_DIR.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension != "json") return@forEach
            
            val relativePath = NOVA.dataFolder.toURI().relativize(file.toURI()).path
            
            if (!existingPaths.contains(relativePath)
                && HashUtils.getFileHash(file, "MD5").contentEquals(UpdatableFile.getStoredHash(file))) {
                
                UpdatableFile.removeStoredHash(file)
                file.delete()
            }
        }
    }
    
    private fun extractWorldGenFile(path: String, addon: AddonLoader? = null): String? {
        val namespace = addon?.description?.id ?: "nova"
        val file = File(NOVA.dataFolder, ".$path").let { File(it.parent, namespace + "_" + it.name) }
        if (file.name.matches(WORLD_GEN_FILE_PATTERN)) {
            UpdatableFile.load(file) { if (addon != null) getResourceAsStream(addon.file, path)!! else getResourceAsStream(path)!! }
            return NOVA.dataFolder.toURI().relativize(file.toURI()).path
        }
        
        LOGGER.severe("Could not load world gen file $path: Invalid file name")
        return null
    }
    
}