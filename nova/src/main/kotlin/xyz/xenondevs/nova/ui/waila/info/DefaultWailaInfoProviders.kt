package xyz.xenondevs.nova.ui.waila.info

import io.papermc.paper.registry.keys.BlockTypeKeys
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.PistonHead
import org.bukkit.block.data.type.TechnicalPiston
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.world.block.blockType

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object DefaultWailaInfoProviders {
    
    // TODO waila icon by block state
    
    // TODO: implement
    
//    val DEFAULT_VANILLA = wailaInfoProvider<BlockData>("default_vanilla") {
//        priority = -1
//        blocks = registryEntrySetOf(RegistryKey.BLOCK)
//        infoProvider { player, pos, blockState ->
//            val mainBlockType = getMainBlockType(blockState)
//            
//            val lines = buildList {
//                this += WailaLine(
//                    Component.translatable(
//                        mainBlockType.localizedName
//                            ?: "block.minecraft.${mainBlockType.name.lowercase()}"
//                    ),
//                    WailaLine.Alignment.CENTERED
//                )
//                
//                this += WailaLine(
//                    Component.text(
//                        "minecraft:${blockState.material.name.lowercase()}",
//                        NamedTextColor.DARK_GRAY
//                    ),
//                    WailaLine.Alignment.CENTERED
//                )
//                this += ToolLine.getToolLine(player, pos)
//            }
//            
//            return@infoProvider WailaInfo(Key.key(mainBlockType.name.lowercase()), lines)
//        }
//    }
//    
//    val DEFAULT_NOVA = wailaInfoProvider("default_nova") {
//        priority = -1
//        blocks = NovaRegistries.BLOCK.entrySet
//        infoProvider { player, pos, blockState ->
//            if (blockState.block in DefaultBlocks.DELEGATES)
//                return@infoProvider DEFAULT_VANILLA.get().getInfo(player, pos, pos.blockData)
//            
//            val blockType = blockState.block
//            val id = blockType.key
//            
//            val lines = ArrayList<WailaLine>()
//            lines += WailaLine(blockType.name, WailaLine.Alignment.CENTERED)
//            lines += WailaLine(Component.text(id.toString(), NamedTextColor.DARK_GRAY), WailaLine.Alignment.CENTERED)
//            lines += ToolLine.getToolLine(player, pos)
//            
//            if (blockState.block is NovaTileEntityBlock) {
//                val tileEntity = WorldDataManager.getTileEntity(pos)
//                if (tileEntity is NetworkedTileEntity) {
//                    val energyHolder = tileEntity.holders.firstInstanceOfOrNull<DefaultEnergyHolder>()
//                    if (energyHolder != null) {
//                        lines += EnergyHolderLine.getEnergyBarLine(energyHolder)
//                        lines += EnergyHolderLine.getEnergyAmountLine(energyHolder)
//                        lines += EnergyHolderLine.getEnergyDeltaLine(energyHolder)
//                    }
//                }
//            }
//            
//            return@infoProvider WailaInfo(id, lines)
//        }
//    }
//    
//    val DEFAULT_TOOL_ICON = wailaToolIconProvider("default_tool_icon") {
//        iconProvider { category, tier ->
//            val name = when (category) {
//                SHEARS -> "shears"
//                SHOVEL, PICKAXE, AXE, HOE, SWORD -> when (tier) {
//                    WOOD, GOLD -> "${tier.key.value()}en_${category.key.value()}"
//                    STONE, COPPER, IRON, DIAMOND, NETHERITE -> "${tier.key.value()}_${category.key.value()}"
//                    else -> null
//                }
//                
//                else -> null
//            }
//            
//            name?.let { Key.key("item/$name") }
//        }
//    }
//    
//    init {
//        wailaInfoProvider<Brushable>("brushable") {
//            blocks = registryEntrySetOf(BlockTypeKeys.SUSPICIOUS_SAND, BlockTypeKeys.SUSPICIOUS_GRAVEL)
//            infoProvider(DEFAULT_VANILLA) { _, _, blockState, info ->
//                info.copy(icon = Key.key("${blockState.material.name.lowercase()}_${blockState.dusted}"))
//            }
//        }
//        
//        wailaInfoProvider<Cake>("brushable") {
//            blocks = registryEntrySetOf(BlockTypeKeys.CAKE)
//            infoProvider(DEFAULT_VANILLA) { _, _, cake, info ->
//                val bites = cake.bites
//                info.copy(icon = Key.key(if (bites == 0) "cake" else "cake_slice$bites"))
//            }
//        }
//        
//        wailaInfoProvider<Campfire>("brushable") {
//            blocks = registryEntrySetOf(BlockTypeKeys.CAMPFIRE, BlockTypeKeys.SOUL_CAMPFIRE)
//            infoProvider(DEFAULT_VANILLA) { _, _, campfire, info ->
//                info.copy(icon = Key.key(if (campfire.isLit) campfire.material.name.lowercase() else "campfire_off"))
//            }
//        }
//        
//        wailaInfoProvider<Candle>("brushable") {
//            blocks = registryEntrySetOf(BlockTypeTagKeys.CANDLES)
//            infoProvider(DEFAULT_VANILLA) { _, _, candle, info ->
//                val name = candle.material.name.lowercase()
//                val amount = when (candle.candles) {
//                    1 -> "one_candle"
//                    2 -> "two_candles"
//                    3 -> "three_candles"
//                    4 -> "four_candles"
//                    else -> IllegalStateException("Invalid amount of candles")
//                }
//                info.copy(icon = Key.key("${name}_${amount}"))
//            }
//        }
//        
//        wailaInfoProvider<Levelled>("cauldron") {
//            blocks = registryEntrySetOf(BlockTypeKeys.WATER_CAULDRON, BlockTypeKeys.POWDER_SNOW_CAULDRON)
//            infoProvider(DEFAULT_VANILLA) { _, _, cauldron, info ->
//                val level = when (cauldron.level) {
//                    1 -> "level1"
//                    2 -> "level2"
//                    3 -> "full"
//                    else -> throw IllegalStateException("Cauldron level is not 1, 2 or 3")
//                }
//                info.copy(icon = Key.key(cauldron.material.name.lowercase() + "_$level"))
//            }
//        }
//        
//        wailaInfoProvider<Cocoa>("cocoa") {
//            blocks = registryEntrySetOf(BlockTypeKeys.COCOA)
//            infoProvider(DEFAULT_VANILLA) { _, _, cocoa, info ->
//                info.copy(icon = Key.key("cocoa_stage${cocoa.age}"))
//            }
//        }
//        
//        wailaInfoProvider<Comparator>("comparator") {
//            blocks = registryEntrySetOf(BlockTypeKeys.COMPARATOR)
//            infoProvider(DEFAULT_VANILLA) { _, _, comparator, info ->
//                info.copy(
//                    icon = Key.key(
//                        "comparator"
//                            + (if (comparator.isPowered) "_on" else "")
//                            + (if (comparator.mode == Comparator.Mode.SUBTRACT) "_subtract" else "")
//                    )
//                )
//            }
//        }
//        
//        wailaInfoProvider<Ageable>("crop") {
//            blocks = registryEntrySetOf(MAX_TEXTURE_STAGES.keys)
//            infoProvider(DEFAULT_VANILLA) { _, _, crop, info ->
//                val maxTexStage = MAX_TEXTURE_STAGES[crop.material.asBlockType()!!.typedKey]!!
//                val stage = ((crop.age / crop.maximumAge.toDouble()) * maxTexStage).roundToInt()
//                info.copy(icon = Key.key(crop.material.name.lowercase() + "_stage$stage"))
//            }
//        }
//        
//        wailaInfoProvider<DaylightDetector>("daylight_detector") {
//            blocks = registryEntrySetOf(BlockTypeKeys.DAYLIGHT_DETECTOR)
//            infoProvider(DEFAULT_VANILLA) { _, _, detector, info ->
//                if (detector.isInverted)
//                    info.copy(icon = Key.key("daylight_detector_inverted"))
//                else info
//            }
//        }
//        
//        wailaInfoProvider<DriedGhast>("dried_ghast") {
//            blocks = registryEntrySetOf(BlockTypeKeys.DRIED_GHAST)
//            infoProvider(DEFAULT_VANILLA) { _, _, driedGhast, info ->
//                info.copy(icon = Key.key("dried_ghast_hydration_${driedGhast.hydration}"))
//            }
//        }
//        
//        wailaInfoProvider<Hatchable>("hatchable") {
//            blocks = registryEntrySetOf(BlockTypeKeys.SNIFFER_EGG)
//            infoProvider(DEFAULT_VANILLA) { _, _, hatchable, info ->
//                info.copy(icon = Key.key("${hatchable.material.name.lowercase()}_${hatchable.hatch}"))
//            }
//        }
//        
//        wailaInfoProvider<Lantern>("lantern") {
//            blocks = registryEntrySetOf(BlockTypeTagKeys.LANTERNS)
//            infoProvider(DEFAULT_VANILLA) { _, _, lantern, info ->
//                if (lantern.isHanging)
//                    info.copy(icon = Key.key(lantern.material.name.lowercase() + "_hanging"))
//                else info
//            }
//        }
//        
//        wailaInfoProvider<Lightable>("redstone_lamp") {
//            blocks = registryEntrySetOf(BlockTypeKeys.REDSTONE_LAMP)
//            infoProvider(DEFAULT_VANILLA) { _, _, lightable, info ->
//                if (lightable.isLit)
//                    info.copy(icon = Key.key("redstone_lamp_on"))
//                else info
//            }
//        }
//        
//        wailaInfoProvider<RedstoneRail>("redstone_rail") {
//            blocks = registryEntrySetOf(BlockTypeKeys.ACTIVATOR_RAIL, BlockTypeKeys.DETECTOR_RAIL, BlockTypeKeys.POWERED_RAIL)
//            infoProvider(DEFAULT_VANILLA) { _, _, rail, info ->
//                info.copy(icon = Key.key(rail.material.name.lowercase() + if (rail.isPowered) "_on" else ""))
//            }
//        }
//        
//        wailaInfoProvider<Repeater>("repeater") {
//            blocks = registryEntrySetOf(BlockTypeKeys.REPEATER)
//            infoProvider(DEFAULT_VANILLA) { _, _, repeater, info ->
//                info.copy(
//                    icon = Key.key(
//                        "repeater_${repeater.delay}tick"
//                            + (if (repeater.isPowered) "_on" else "")
//                            + (if (repeater.isLocked) "_locked" else "")
//                    )
//                )
//            }
//        }
//        
//        wailaInfoProvider<RespawnAnchor>("respawn_anchor") {
//            blocks = registryEntrySetOf(BlockTypeKeys.RESPAWN_ANCHOR)
//            infoProvider(DEFAULT_VANILLA) { _, _, anchor, info ->
//                info.copy(icon = Key.key("respawn_anchor_${anchor.charges}"))
//            }
//        }
//        
//        wailaInfoProvider<SeaPickle>("sea_pickle") {
//            blocks = registryEntrySetOf(BlockTypeKeys.SEA_PICKLE)
//            infoProvider(DEFAULT_VANILLA) { _, _, pickle, info ->
//                fun getSeaPickleName(): String {
//                    val amount = pickle.pickles
//                    if (amount > 1) {
//                        val prefix = when (amount) {
//                            2 -> "two"
//                            3 -> "three"
//                            4 -> "four"
//                            else -> throw IllegalStateException("Invalid amount: $amount")
//                        }
//                        
//                        return prefix + (if (!pickle.isWaterlogged) "_dead_" else "_") + "sea_pickles"
//                    }
//                    
//                    return (if (!pickle.isWaterlogged) "dead_" else "") + "sea_pickle"
//                }
//                
//                info.copy(icon = Key.key(getSeaPickleName()))
//            }
//        }
//        
//        wailaInfoProvider<TestBlock>("test_block") {
//            blocks = registryEntrySetOf(BlockTypeKeys.TEST_BLOCK)
//            infoProvider(DEFAULT_VANILLA) { _, _, testBlock, info ->
//                info.copy(icon = Key.key("test_block_${testBlock.mode.name.lowercase()}"))
//            }
//        }
//    }
    
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

private fun getMainBlockType(blockState: BlockData): BlockType {
    return when (val type = blockState.blockType) {
        // infested blocks
        BlockType.INFESTED_CHISELED_STONE_BRICKS -> BlockType.CHISELED_STONE_BRICKS
        BlockType.INFESTED_COBBLESTONE -> BlockType.COBBLESTONE
        BlockType.INFESTED_CRACKED_STONE_BRICKS -> BlockType.STONE_BRICKS
        BlockType.INFESTED_DEEPSLATE -> BlockType.DEEPSLATE
        BlockType.INFESTED_MOSSY_STONE_BRICKS -> BlockType.MOSSY_STONE_BRICKS
        BlockType.INFESTED_STONE -> BlockType.STONE
        BlockType.INFESTED_STONE_BRICKS -> BlockType.STONE_BRICKS
        
        // signs
        BlockType.OAK_WALL_SIGN -> BlockType.OAK_SIGN
        BlockType.SPRUCE_WALL_SIGN -> BlockType.SPRUCE_SIGN
        BlockType.BIRCH_WALL_SIGN -> BlockType.BIRCH_SIGN
        BlockType.JUNGLE_WALL_SIGN -> BlockType.JUNGLE_SIGN
        BlockType.ACACIA_WALL_SIGN -> BlockType.ACACIA_SIGN
        BlockType.DARK_OAK_WALL_SIGN -> BlockType.DARK_OAK_SIGN
        BlockType.MANGROVE_WALL_SIGN -> BlockType.MANGROVE_SIGN
        BlockType.CRIMSON_WALL_SIGN -> BlockType.CRIMSON_SIGN
        BlockType.WARPED_WALL_SIGN -> BlockType.WARPED_SIGN
        BlockType.BAMBOO_WALL_SIGN -> BlockType.BAMBOO_SIGN
        BlockType.CHERRY_WALL_SIGN -> BlockType.CHERRY_SIGN
        BlockType.OAK_WALL_HANGING_SIGN -> BlockType.OAK_HANGING_SIGN
        BlockType.SPRUCE_WALL_HANGING_SIGN -> BlockType.SPRUCE_HANGING_SIGN
        BlockType.BIRCH_WALL_HANGING_SIGN -> BlockType.BIRCH_HANGING_SIGN
        BlockType.JUNGLE_WALL_HANGING_SIGN -> BlockType.JUNGLE_HANGING_SIGN
        BlockType.ACACIA_WALL_HANGING_SIGN -> BlockType.ACACIA_HANGING_SIGN
        BlockType.DARK_OAK_WALL_HANGING_SIGN -> BlockType.DARK_OAK_HANGING_SIGN
        BlockType.MANGROVE_WALL_HANGING_SIGN -> BlockType.MANGROVE_HANGING_SIGN
        BlockType.CRIMSON_WALL_HANGING_SIGN -> BlockType.CRIMSON_HANGING_SIGN
        BlockType.WARPED_WALL_HANGING_SIGN -> BlockType.WARPED_HANGING_SIGN
        BlockType.BAMBOO_WALL_HANGING_SIGN -> BlockType.BAMBOO_HANGING_SIGN
        BlockType.CHERRY_WALL_HANGING_SIGN -> BlockType.CHERRY_HANGING_SIGN
        
        // plant
        BlockType.WEEPING_VINES_PLANT -> BlockType.WEEPING_VINES
        BlockType.TWISTING_VINES_PLANT -> BlockType.TWISTING_VINES
        BlockType.KELP_PLANT -> BlockType.KELP
        BlockType.ATTACHED_MELON_STEM -> BlockType.MELON_STEM
        BlockType.ATTACHED_PUMPKIN_STEM -> BlockType.PUMPKIN_STEM
        
        // torch
        BlockType.WALL_TORCH -> BlockType.TORCH
        BlockType.REDSTONE_WALL_TORCH -> BlockType.REDSTONE_TORCH
        BlockType.SOUL_WALL_TORCH -> BlockType.SOUL_TORCH
        BlockType.COPPER_WALL_TORCH -> BlockType.COPPER_TORCH
        
        // head / skull
        BlockType.ZOMBIE_WALL_HEAD -> BlockType.ZOMBIE_HEAD
        BlockType.CREEPER_WALL_HEAD -> BlockType.CREEPER_HEAD
        BlockType.PLAYER_WALL_HEAD -> BlockType.PLAYER_HEAD
        BlockType.SKELETON_WALL_SKULL -> BlockType.SKELETON_SKULL
        BlockType.WITHER_SKELETON_WALL_SKULL -> BlockType.WITHER_SKELETON_SKULL
        BlockType.DRAGON_WALL_HEAD -> BlockType.DRAGON_HEAD
        
        // misc
        BlockType.BIG_DRIPLEAF_STEM -> BlockType.BIG_DRIPLEAF
        BlockType.PISTON_HEAD -> {
            blockState as PistonHead
            if (blockState.type == TechnicalPiston.Type.STICKY)
                BlockType.STICKY_PISTON
            else BlockType.PISTON
        }
        
        else -> type
    }
}