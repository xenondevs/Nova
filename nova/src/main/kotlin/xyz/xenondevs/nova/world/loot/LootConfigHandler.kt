package xyz.xenondevs.nova.world.loot

import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.serialization.json.GSON
import xyz.xenondevs.nova.util.data.UpdatableFile
import kotlin.io.path.extension
import kotlin.io.path.name

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object LootConfigHandler {
    
    @InitFun
    private suspend fun init() {
        UpdatableFile.extractIdNamedFromAllAddons("loot") { it.extension == "json" }.forEach { [file, data] ->
            GSON.fromJson<LootTable>(data.decodeToString())
                .also { if (it == null) LOGGER.error("Failed to load loot table ${file.name}") }
                ?.let(LootGeneration::register)
        }
    }
    
}