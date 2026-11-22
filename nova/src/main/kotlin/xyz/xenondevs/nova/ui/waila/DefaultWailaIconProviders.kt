package xyz.xenondevs.nova.ui.waila

import io.papermc.paper.registry.RegistryKey
import org.bukkit.block.BlockType
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Directional
import org.bukkit.block.data.type.DaylightDetector
import org.bukkit.block.data.type.DriedGhast
import xyz.xenondevs.commons.provider.plus
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistrar
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.entries.BlockTypeEntries
import xyz.xenondevs.nova.registry.entry
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.itemTypeOrNull
import kotlin.math.roundToInt

@InternalInit(
    stage = BOOTSTRAP,
    runBefore = [RegistryLoader::class]
)
internal object DefaultWailaIconProviders {
    
    val ITEM = NovaRegistrar.wailaIconProvider<BlockData>("item") {
        priority = -2
        blocks = registryEntrySetOf(RegistryKey.BLOCK)
        iconProvider { blockState ->
            blockState.blockType.itemTypeOrNull?.let(WailaIcon::Item)
        }
    }
    
    val BLOCK_STATE = NovaRegistrar.wailaIconProvider("block_state") {
        priority = -1
        blocks += registryEntrySetOf(
            BlockTypeEntries.SUSPICIOUS_SAND, BlockTypeEntries.SUSPICIOUS_GRAVEL,
            BlockTypeEntries.CAKE, 
            BlockTypeEntries.WATER_CAULDRON, BlockTypeEntries.POWDER_SNOW_CAULDRON,
            BlockTypeEntries.SNIFFER_EGG,
            BlockTypeEntries.REDSTONE_LAMP,
            BlockTypeEntries.RESPAWN_ANCHOR,
            BlockTypeEntries.TEST_BLOCK
        )
        blocks += BlockTypeTags.CANDLE_CAKES
        
        iconProvider(WailaIcon::BlockState)
    }
    
    val DIRECTIONAL_BLOCK_STATE = NovaRegistrar.wailaIconProvider<Directional>("directional_block_state") {
        priority = -1
        blocks += registryEntrySetOf(
            BlockTypeEntries.COCOA,
            BlockTypeEntries.COMPARATOR, 
            BlockTypeEntries.REPEATER
        )
        iconProvider { WailaIcon.BlockState(it.apply { facing = SOUTH }) }
    }
    
    val FIT_BLOCK_STATE = NovaRegistrar.wailaIconProvider("fit_block_state") {
        priority = -1
        blocks += BlockTypeEntries.SEA_PICKLE
        blocks += BlockTypeTags.CANDLES
        iconProvider { WailaIcon.BlockState(it, WailaIcon.Framing.Fit(0f)) }
    }
    
    val DAYLIGHT_DETECTOR = NovaRegistrar.wailaIconProvider<DaylightDetector>("daylight_detector") {
        priority = -1
        blocks = registryEntrySetOf(BlockTypeEntries.DAYLIGHT_DETECTOR)
        iconProvider { WailaIcon.BlockState(it.apply { power = 0 }) }
    }
    
    val DRIED_GHAST = NovaRegistrar.wailaIconProvider<DriedGhast>("dried_ghast") {
        priority = -1
        blocks = registryEntrySetOf(BlockTypeEntries.DRIED_GHAST)
        iconProvider { state ->
            WailaIcon.BlockState(state.apply { isWaterlogged = false; facing = NORTH })
        }
    }
    
    val TEXTURE = NovaRegistrar.wailaIconProvider<Ageable>("texture") {
        priority = -1
        blocks = registryEntrySetOf(MAX_TEXTURE_STAGES.keys)
        iconProvider { crop ->
            val maxTexStage = MAX_TEXTURE_STAGES[crop.blockType.entry]!!
            val stage = ((crop.age / crop.maximumAge.toDouble()) * maxTexStage).roundToInt()
            val texture = if (crop.blockType == BlockType.TORCHFLOWER_CROP && stage == 2)
                "torchflower"
            else "${crop.blockType.key.value()}_stage$stage"
            WailaIcon.Texture(ResourcePath(ResourceType.Texture, "minecraft", "block/$texture"))
        }
    }
    
}

private val MAX_TEXTURE_STAGES = mapOf(
    BlockTypeEntries.BEETROOTS to 3,
    BlockTypeEntries.CARROTS to 3,
    BlockTypeEntries.NETHER_WART to 2,
    BlockTypeEntries.POTATOES to 3,
    BlockTypeEntries.WHEAT to 7,
    BlockTypeEntries.SWEET_BERRY_BUSH to 3,
    BlockTypeEntries.TORCHFLOWER_CROP to 2
)
