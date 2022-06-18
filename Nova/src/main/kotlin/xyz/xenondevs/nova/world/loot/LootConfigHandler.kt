package xyz.xenondevs.nova.world.loot

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.addon.loader.AddonLoader
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.data.*
import java.io.File

private val LOOT_DIRECTORY = File(NOVA.dataFolder, "loot")
private val LOOT_FILE_PATTERN = Regex("""^[a-z][a-z\d_]*.json$""")

internal object LootConfigHandler : Initializable() {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    override fun init() {
        extractLootTables()
        loadLootTables()
    }
    
    private fun extractLootTables() {
        val extracted = mutableListOf<String>()
        
        extracted += getResources("loot/").mapNotNull(::extractLootTable)
        
        AddonManager.loaders.values.forEach { loader ->
            extracted += getResources(loader.file, "loot/").mapNotNull { extractLootTable(it, loader) }
        }
        
        LOOT_DIRECTORY.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension != "json") return@forEach
            
            val relativePath = NOVA.dataFolder.toURI().relativize(file.toURI()).path
            
            if (relativePath !in extracted
                && HashUtils.getFileHash(file, "MD5").contentEquals(UpdatableFile.getStoredHash(file))) {
                
                UpdatableFile.removeStoredHash(file)
                file.delete()
            }
        }
    }
    
    private fun extractLootTable(path: String, addon: AddonLoader? = null): String? {
        val namespace = addon?.description?.id ?: "nova"
        val file = File(NOVA.dataFolder, path).let { File(it.parent, namespace + "_" + it.name) }
        if (file.name.matches(LOOT_FILE_PATTERN)) {
            UpdatableFile.load(file) { if (addon != null) getResourceAsStream(addon.file, path)!! else getResourceAsStream(path)!! }
            return NOVA.dataFolder.toURI().relativize(file.toURI()).path
        }
        
        LOGGER.severe("Could not load loot file $path: Invalid file name")
        return null
    }
    
    private fun loadLootTables() {
        LOOT_DIRECTORY.walkTopDown().forEach { file ->
            if (file.isDirectory || file.extension != "json") return@forEach
            
            val lootTable = GSON.fromJson<LootTable>(file.reader())
            if (lootTable == null) {
                LOGGER.severe("Failed to load loot table ${file.name}")
                return@forEach
            }
            
            LootGeneration.register(lootTable)
        }
    }
    
}