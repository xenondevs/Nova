package xyz.xenondevs.nova.world.loot

import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.data.UpdatableFile
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.serialization.json.GSON
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object LootConfigHandler {
    
    @InitFun
    private fun init() {
        UpdatableFile.extractIdNamedFromAllAddons("loot")
        loadLootTables()
    }
    
    private fun loadLootTables() {
        for (addon in AddonBootstrapper.addons) {
            addon.dataFolder.resolve("loot").walk()
                .filter { it.isRegularFile() && it.extension == "json" && ResourcePath.isValidPath(it.name) }
                .forEach { file ->
                    val lootTable = GSON.fromJson<LootTable>(file)
                    if (lootTable == null) {
                        LOGGER.error("Failed to load loot table ${file.name}")
                        return@forEach
                    }
                    
                    LootGeneration.register(lootTable)
                }
        }
    }
    
}