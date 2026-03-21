package xyz.xenondevs.nova.ui.waila.info

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.BlockTypeKeys
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.Brushable
import org.bukkit.block.data.Hatchable
import org.bukkit.block.data.Levelled
import org.bukkit.block.data.Lightable
import org.bukkit.block.data.type.Cake
import org.bukkit.block.data.type.Campfire
import org.bukkit.block.data.type.Candle
import org.bukkit.block.data.type.Cocoa
import org.bukkit.block.data.type.Comparator
import org.bukkit.block.data.type.DaylightDetector
import org.bukkit.block.data.type.DriedGhast
import org.bukkit.block.data.type.Lantern
import org.bukkit.block.data.type.PistonHead
import org.bukkit.block.data.type.RedstoneRail
import org.bukkit.block.data.type.Repeater
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.block.data.type.SeaPickle
import org.bukkit.block.data.type.TechnicalPiston
import org.bukkit.block.data.type.TestBlock
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.wailaInfoProvider
import xyz.xenondevs.nova.registry.NovaRegistrar.wailaToolIconProvider
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.registry.typedKey
import xyz.xenondevs.nova.util.item.localizedName
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.AXE
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.HOE
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.PICKAXE
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.SHEARS
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.SHOVEL
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories.SWORD
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.COPPER
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.DIAMOND
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.GOLD
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.IRON
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.NETHERITE
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.STONE
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers.WOOD
import kotlin.math.roundToInt

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object DefaultWailaInfoProviders {
    
    // TODO waila icon by block state
    
    val DEFAULT_VANILLA = wailaInfoProvider<BlockData>("default_vanilla") {
        priority = -1
        blocks = registryEntrySetOf(RegistryKey.BLOCK)
        infoProvider { player, pos, blockState ->
            val mainMaterial = getMainMaterial(blockState)
            
            val lines = buildList {
                this += WailaLine(
                    Component.translatable(
                        mainMaterial.localizedName
                            ?: "block.minecraft.${mainMaterial.name.lowercase()}"
                    ),
                    WailaLine.Alignment.CENTERED
                )
                
                this += WailaLine(
                    Component.text(
                        "minecraft:${blockState.material.name.lowercase()}",
                        NamedTextColor.DARK_GRAY
                    ),
                    WailaLine.Alignment.CENTERED
                )
                this += ToolLine.getToolLine(player, pos.block)
            }
            
            return@infoProvider WailaInfo(Key.key(mainMaterial.name.lowercase()), lines)
        }
    }
    
    val DEFAULT_NOVA = wailaInfoProvider("default_nova") {
        priority = -1
        blocks = NovaRegistries.BLOCK.entrySet
        infoProvider { player, pos, blockState ->
            if (blockState.block in DefaultBlocks.DELEGATES)
                return@infoProvider DEFAULT_VANILLA.get().getInfo(player, pos, pos.block.blockData)
            
            val blockType = blockState.block
            val id = blockType.key
            
            val lines = ArrayList<WailaLine>()
            lines += WailaLine(blockType.name, WailaLine.Alignment.CENTERED)
            lines += WailaLine(Component.text(id.toString(), NamedTextColor.DARK_GRAY), WailaLine.Alignment.CENTERED)
            lines += ToolLine.getToolLine(player, pos.block)
            
            if (blockState.block is NovaTileEntityBlock) {
                val tileEntity = WorldDataManager.getTileEntity(pos)
                if (tileEntity is NetworkedTileEntity) {
                    val energyHolder = tileEntity.holders.firstInstanceOfOrNull<DefaultEnergyHolder>()
                    if (energyHolder != null) {
                        lines += EnergyHolderLine.getEnergyBarLine(energyHolder)
                        lines += EnergyHolderLine.getEnergyAmountLine(energyHolder)
                        lines += EnergyHolderLine.getEnergyDeltaLine(energyHolder)
                    }
                }
            }
            
            return@infoProvider WailaInfo(id, lines)
        }
    }
    
    val DEFAULT_TOOL_ICON = wailaToolIconProvider("default_tool_icon") {
        iconProvider { category, tier ->
            val name = when (category) {
                SHEARS -> "shears"
                SHOVEL, PICKAXE, AXE, HOE, SWORD -> when (tier) {
                    WOOD, GOLD -> "${tier.key.value()}en_${category.key.value()}"
                    STONE, COPPER, IRON, DIAMOND, NETHERITE -> "${tier.key.value()}_${category.key.value()}"
                    else -> null
                }
                
                else -> null
            }
            
            name?.let { Key.key("item/$name") }
        }
    }
    
    init {
        wailaInfoProvider<Brushable>("brushable") {
            blocks = registryEntrySetOf(BlockTypeKeys.SUSPICIOUS_SAND, BlockTypeKeys.SUSPICIOUS_GRAVEL)
            infoProvider(DEFAULT_VANILLA) { _, _, blockState, info ->
                info.copy(icon = Key.key("${blockState.material.name.lowercase()}_${blockState.dusted}"))
            }
        }
        
        wailaInfoProvider<Cake>("brushable") {
            blocks = registryEntrySetOf(BlockTypeKeys.CAKE)
            infoProvider(DEFAULT_VANILLA) { _, _, cake, info ->
                val bites = cake.bites
                info.copy(icon = Key.key(if (bites == 0) "cake" else "cake_slice$bites"))
            }
        }
        
        wailaInfoProvider<Campfire>("brushable") {
            blocks = registryEntrySetOf(BlockTypeKeys.CAMPFIRE, BlockTypeKeys.SOUL_CAMPFIRE)
            infoProvider(DEFAULT_VANILLA) { _, _, campfire, info ->
                info.copy(icon = Key.key(if (campfire.isLit) campfire.material.name.lowercase() else "campfire_off"))
            }
        }
        
        wailaInfoProvider<Candle>("brushable") {
            blocks = registryEntrySetOf(BlockTypeTagKeys.CANDLES)
            infoProvider(DEFAULT_VANILLA) { _, _, candle, info ->
                val name = candle.material.name.lowercase()
                val amount = when (candle.candles) {
                    1 -> "one_candle"
                    2 -> "two_candles"
                    3 -> "three_candles"
                    4 -> "four_candles"
                    else -> IllegalStateException("Invalid amount of candles")
                }
                info.copy(icon = Key.key("${name}_${amount}"))
            }
        }
        
        wailaInfoProvider<Levelled>("cauldron") {
            blocks = registryEntrySetOf(BlockTypeKeys.WATER_CAULDRON, BlockTypeKeys.POWDER_SNOW_CAULDRON)
            infoProvider(DEFAULT_VANILLA) { _, _, cauldron, info ->
                val level = when (cauldron.level) {
                    1 -> "level1"
                    2 -> "level2"
                    3 -> "full"
                    else -> throw IllegalStateException("Cauldron level is not 1, 2 or 3")
                }
                info.copy(icon = Key.key(cauldron.material.name.lowercase() + "_$level"))
            }
        }
        
        wailaInfoProvider<Cocoa>("cocoa") {
            blocks = registryEntrySetOf(BlockTypeKeys.COCOA)
            infoProvider(DEFAULT_VANILLA) { _, _, cocoa, info ->
                info.copy(icon = Key.key("cocoa_stage${cocoa.age}"))
            }
        }
        
        wailaInfoProvider<Comparator>("comparator") {
            blocks = registryEntrySetOf(BlockTypeKeys.COMPARATOR)
            infoProvider(DEFAULT_VANILLA) { _, _, comparator, info ->
                info.copy(
                    icon = Key.key(
                        "comparator"
                            + (if (comparator.isPowered) "_on" else "")
                            + (if (comparator.mode == Comparator.Mode.SUBTRACT) "_subtract" else "")
                    )
                )
            }
        }
        
        wailaInfoProvider<Ageable>("crop") {
            blocks = registryEntrySetOf(MAX_TEXTURE_STAGES.keys)
            infoProvider(DEFAULT_VANILLA) { _, _, crop, info ->
                val maxTexStage = MAX_TEXTURE_STAGES[crop.material.asBlockType()!!.typedKey]!!
                val stage = ((crop.age / crop.maximumAge.toDouble()) * maxTexStage).roundToInt()
                info.copy(icon = Key.key(crop.material.name.lowercase() + "_stage$stage"))
            }
        }
        
        wailaInfoProvider<DaylightDetector>("daylight_detector") {
            blocks = registryEntrySetOf(BlockTypeKeys.DAYLIGHT_DETECTOR)
            infoProvider(DEFAULT_VANILLA) { _, _, detector, info ->
                if (detector.isInverted)
                    info.copy(icon = Key.key("daylight_detector_inverted"))
                else info
            }
        }
        
        wailaInfoProvider<DriedGhast>("dried_ghast") {
            blocks = registryEntrySetOf(BlockTypeKeys.DRIED_GHAST)
            infoProvider(DEFAULT_VANILLA) { _, _, driedGhast, info ->
                info.copy(icon = Key.key("dried_ghast_hydration_${driedGhast.hydration}"))
            }
        }
        
        wailaInfoProvider<Hatchable>("hatchable") {
            blocks = registryEntrySetOf(BlockTypeKeys.SNIFFER_EGG)
            infoProvider(DEFAULT_VANILLA) { _, _, hatchable, info ->
                info.copy(icon = Key.key("${hatchable.material.name.lowercase()}_${hatchable.hatch}"))
            }
        }
        
        wailaInfoProvider<Lantern>("lantern") {
            blocks = registryEntrySetOf(BlockTypeTagKeys.LANTERNS)
            infoProvider(DEFAULT_VANILLA) { _, _, lantern, info ->
                if (lantern.isHanging)
                    info.copy(icon = Key.key(lantern.material.name.lowercase() + "_hanging"))
                else info
            }
        }
        
        wailaInfoProvider<Lightable>("redstone_lamp") {
            blocks = registryEntrySetOf(BlockTypeKeys.REDSTONE_LAMP)
            infoProvider(DEFAULT_VANILLA) { _, _, lightable, info ->
                if (lightable.isLit)
                    info.copy(icon = Key.key("redstone_lamp_on"))
                else info
            }
        }
        
        wailaInfoProvider<RedstoneRail>("redstone_rail") {
            blocks = registryEntrySetOf(BlockTypeKeys.ACTIVATOR_RAIL, BlockTypeKeys.DETECTOR_RAIL, BlockTypeKeys.POWERED_RAIL)
            infoProvider(DEFAULT_VANILLA) { _, _, rail, info ->
                info.copy(icon = Key.key(rail.material.name.lowercase() + if (rail.isPowered) "_on" else ""))
            }
        }
        
        wailaInfoProvider<Repeater>("repeater") {
            blocks = registryEntrySetOf(BlockTypeKeys.REPEATER)
            infoProvider(DEFAULT_VANILLA) { _, _, repeater, info ->
                info.copy(
                    icon = Key.key(
                        "repeater_${repeater.delay}tick"
                            + (if (repeater.isPowered) "_on" else "")
                            + (if (repeater.isLocked) "_locked" else "")
                    )
                )
            }
        }
        
        wailaInfoProvider<RespawnAnchor>("respawn_anchor") {
            blocks = registryEntrySetOf(BlockTypeKeys.RESPAWN_ANCHOR)
            infoProvider(DEFAULT_VANILLA) { _, _, anchor, info ->
                info.copy(icon = Key.key("respawn_anchor_${anchor.charges}"))
            }
        }
        
        wailaInfoProvider<SeaPickle>("sea_pickle") {
            blocks = registryEntrySetOf(BlockTypeKeys.SEA_PICKLE)
            infoProvider(DEFAULT_VANILLA) { _, _, pickle, info ->
                fun getSeaPickleName(): String {
                    val amount = pickle.pickles
                    if (amount > 1) {
                        val prefix = when (amount) {
                            2 -> "two"
                            3 -> "three"
                            4 -> "four"
                            else -> throw IllegalStateException("Invalid amount: $amount")
                        }
                        
                        return prefix + (if (!pickle.isWaterlogged) "_dead_" else "_") + "sea_pickles"
                    }
                    
                    return (if (!pickle.isWaterlogged) "dead_" else "") + "sea_pickle"
                }
                
                info.copy(icon = Key.key(getSeaPickleName()))
            }
        }
        
        wailaInfoProvider<TestBlock>("test_block") {
            blocks = registryEntrySetOf(BlockTypeKeys.TEST_BLOCK)
            infoProvider(DEFAULT_VANILLA) { _, _, testBlock, info ->
                info.copy(icon = Key.key("test_block_${testBlock.mode.name.lowercase()}"))
            }
        }
    }
    
}

private val MAX_TEXTURE_STAGES = mapOf(
    BlockTypeKeys.BEETROOTS to 3,
    BlockTypeKeys.CARROTS to 3,
    BlockTypeKeys.NETHER_WART to 2,
    BlockTypeKeys.POTATOES to 3,
    BlockTypeKeys.WHEAT to 7,
    BlockTypeKeys.SWEET_BERRY_BUSH to 3,
    BlockTypeKeys.TORCHFLOWER_CROP to 2
)

private fun getMainMaterial(blockState: BlockData): Material {
    return when (val material = blockState.material) {
        // infested blocks
        Material.INFESTED_CHISELED_STONE_BRICKS -> Material.CHISELED_STONE_BRICKS
        Material.INFESTED_COBBLESTONE -> Material.COBBLESTONE
        Material.INFESTED_CRACKED_STONE_BRICKS -> Material.STONE_BRICKS
        Material.INFESTED_DEEPSLATE -> Material.DEEPSLATE
        Material.INFESTED_MOSSY_STONE_BRICKS -> Material.MOSSY_STONE_BRICKS
        Material.INFESTED_STONE -> Material.STONE
        Material.INFESTED_STONE_BRICKS -> Material.STONE_BRICKS
        
        // signs
        Material.OAK_WALL_SIGN -> Material.OAK_SIGN
        Material.SPRUCE_WALL_SIGN -> Material.SPRUCE_SIGN
        Material.BIRCH_WALL_SIGN -> Material.BIRCH_SIGN
        Material.JUNGLE_WALL_SIGN -> Material.JUNGLE_SIGN
        Material.ACACIA_WALL_SIGN -> Material.ACACIA_SIGN
        Material.DARK_OAK_WALL_SIGN -> Material.DARK_OAK_SIGN
        Material.MANGROVE_WALL_SIGN -> Material.MANGROVE_SIGN
        Material.CRIMSON_WALL_SIGN -> Material.CRIMSON_SIGN
        Material.WARPED_WALL_SIGN -> Material.WARPED_SIGN
        Material.BAMBOO_WALL_SIGN -> Material.BAMBOO_SIGN
        Material.CHERRY_WALL_SIGN -> Material.CHERRY_SIGN
        Material.OAK_WALL_HANGING_SIGN -> Material.OAK_HANGING_SIGN
        Material.SPRUCE_WALL_HANGING_SIGN -> Material.SPRUCE_HANGING_SIGN
        Material.BIRCH_WALL_HANGING_SIGN -> Material.BIRCH_HANGING_SIGN
        Material.JUNGLE_WALL_HANGING_SIGN -> Material.JUNGLE_HANGING_SIGN
        Material.ACACIA_WALL_HANGING_SIGN -> Material.ACACIA_HANGING_SIGN
        Material.DARK_OAK_WALL_HANGING_SIGN -> Material.DARK_OAK_HANGING_SIGN
        Material.MANGROVE_WALL_HANGING_SIGN -> Material.MANGROVE_HANGING_SIGN
        Material.CRIMSON_WALL_HANGING_SIGN -> Material.CRIMSON_HANGING_SIGN
        Material.WARPED_WALL_HANGING_SIGN -> Material.WARPED_HANGING_SIGN
        Material.BAMBOO_WALL_HANGING_SIGN -> Material.BAMBOO_HANGING_SIGN
        Material.CHERRY_WALL_HANGING_SIGN -> Material.CHERRY_HANGING_SIGN
        
        // plant
        Material.WEEPING_VINES_PLANT -> Material.WEEPING_VINES
        Material.TWISTING_VINES_PLANT -> Material.TWISTING_VINES
        Material.KELP_PLANT -> Material.KELP
        Material.ATTACHED_MELON_STEM -> Material.MELON_STEM
        Material.ATTACHED_PUMPKIN_STEM -> Material.PUMPKIN_STEM
        
        // torch
        Material.WALL_TORCH -> Material.TORCH
        Material.REDSTONE_WALL_TORCH -> Material.REDSTONE_TORCH
        Material.SOUL_WALL_TORCH -> Material.SOUL_TORCH
        Material.COPPER_WALL_TORCH -> Material.COPPER_TORCH
        
        // head / skull
        Material.ZOMBIE_WALL_HEAD -> Material.ZOMBIE_HEAD
        Material.CREEPER_WALL_HEAD -> Material.CREEPER_HEAD
        Material.PLAYER_WALL_HEAD -> Material.PLAYER_HEAD
        Material.SKELETON_WALL_SKULL -> Material.SKELETON_SKULL
        Material.WITHER_SKELETON_WALL_SKULL -> Material.WITHER_SKELETON_SKULL
        Material.DRAGON_WALL_HEAD -> Material.DRAGON_HEAD
        
        // misc
        Material.BIG_DRIPLEAF_STEM -> Material.BIG_DRIPLEAF
        Material.TRIPWIRE -> Material.STRING
        Material.PISTON_HEAD -> {
            blockState as PistonHead
            if (blockState.type == TechnicalPiston.Type.STICKY)
                Material.STICKY_PISTON
            else Material.PISTON
        }
        
        else -> material
    }
}