package xyz.xenondevs.nova.ui.waila.info

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.BlockTypeKeys
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockType
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
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistrar.wailaInfoProvider
import xyz.xenondevs.nova.registry.NovaRegistrar.wailaToolIconProvider
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.registry.typedKey
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.resources.builder.task.WailaEnergyBarTextures
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.util.capitalizeAll
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.toMinecraftLocaleCode
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.isNovaTileEntity
import xyz.xenondevs.nova.world.block.name
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import kotlin.math.roundToInt

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
object DefaultWailaInfoProviders {
    
    val DEFAULT = wailaInfoProvider("default") {
        priority = -1
        blocks = registryEntrySetOf(RegistryKey.BLOCK)
        infoProvider { player, block, blockState ->
            val blockType = getMainBlockType(blockState)
            
            val lines = buildList {
                val firstLine = Component.text()
                    .append(blockType.name)
                    .append(Component.text(" "))
                    .append(getToolText(player, block))
                    .build()
                this += WailaLine(
                    firstLine,
                    WailaLine.Alignment.LEFT
                )
                
                if (blockType.isNovaTileEntity) {
                    val tileEntity = block.novaTileEntity
                    if (tileEntity is NetworkedTileEntity) {
                        val energyHolder = tileEntity.holders.firstInstanceOfOrNull<DefaultEnergyHolder>()
                        if (energyHolder != null) {
                            val firstLineWidth = CharSizes.calculateComponentWidth(blockType.name, player.locale().toMinecraftLocaleCode())
                            this += WailaLine(
                                createEnergyBar(energyHolder, firstLineWidth),
                                WailaLine.Alignment.LEFT
                            )
                        }
                    }
                }
                
                this += WailaLine(
                    Component.text()
                        .append(Component.text(
                            blockType.key.namespace
                                .replace('_', ' ')
                                .replace('-', ' ')
                                .capitalizeAll(),
                            NamedTextColor.BLUE,
                            TextDecoration.ITALIC
                        ))
                        .shadowColor(ShadowColor.none())
                        .build(),
                    WailaLine.Alignment.LEFT
                )
                
            }
            
            return@infoProvider WailaInfo(blockType.key, lines)
        }
    }
    
    val DEFAULT_TOOL_ICON = wailaToolIconProvider("default_tool_icon") {
        iconProvider { tags ->
            buildSet {
                if (BlockTypeTags.SHEARS_MINOR_BREAKING_SPEED in tags || BlockTypeTags.SHEARS_MAJOR_BREAKING_SPEED in tags || BlockTypeTags.SHEARS_EXTREME_BREAKING_SPEED in tags)
                    add(Key.key("item/shears"))
                if (BlockTypeTags.SWORD_EFFICIENT in tags || BlockTypeTags.SWORD_INSTANTLY_MINES in tags)
                    add(Key.key("item/wooden_sword"))
                
                fun toolType(name: String, typeTag: RegistryEntrySet.Paper.Tag<BlockType>) {
                    if (typeTag !in tags)
                        return
                    when {
                        BlockTypeTags.NEEDS_STONE_TOOL in tags -> add(Key.key("item/stone_$name"))
                        BlockTypeTags.NEEDS_IRON_TOOL in tags -> add(Key.key("item/iron_$name"))
                        BlockTypeTags.NEEDS_DIAMOND_TOOL in tags -> add(Key.key("item/diamond_$name"))
                        BlockTypeTags.INCORRECT_FOR_WOODEN_TOOL !in tags -> add(Key.key("item/wooden_$name"))
                        else -> Unit
                    }
                }
                
                toolType("shovel", BlockTypeTags.MINEABLE_SHOVEL)
                toolType("pickaxe", BlockTypeTags.MINEABLE_PICKAXE)
                toolType("axe", BlockTypeTags.MINEABLE_AXE)
                toolType("hoe", BlockTypeTags.MINEABLE_HOE)
            }
            
        }
    }
    
    init {
        wailaInfoProvider<Brushable>("brushable") {
            blocks = registryEntrySetOf(BlockTypeKeys.SUSPICIOUS_SAND, BlockTypeKeys.SUSPICIOUS_GRAVEL)
            infoProvider(DEFAULT) { _, _, blockState, info ->
                info.copy(icon = Key.key("${blockState.blockType.key().value()}_${blockState.dusted}"))
            }
        }
        
        wailaInfoProvider<Cake>("brushable") {
            blocks = registryEntrySetOf(BlockTypeKeys.CAKE)
            infoProvider(DEFAULT) { _, _, cake, info ->
                val bites = cake.bites
                info.copy(icon = Key.key(if (bites == 0) "cake" else "cake_slice$bites"))
            }
        }
        
        wailaInfoProvider<Campfire>("brushable") {
            blocks = registryEntrySetOf(BlockTypeKeys.CAMPFIRE, BlockTypeKeys.SOUL_CAMPFIRE)
            infoProvider(DEFAULT) { _, _, campfire, info ->
                info.copy(icon = Key.key(if (campfire.isLit) campfire.blockType.key().value() else "campfire_off"))
            }
        }
        
        wailaInfoProvider<Candle>("brushable") {
            blocks = registryEntrySetOf(BlockTypeTagKeys.CANDLES)
            infoProvider(DEFAULT) { _, _, candle, info ->
                val name = candle.blockType.key().value()
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
            infoProvider(DEFAULT) { _, _, cauldron, info ->
                val level = when (cauldron.level) {
                    1 -> "level1"
                    2 -> "level2"
                    3 -> "full"
                    else -> throw IllegalStateException("Cauldron level is not 1, 2 or 3")
                }
                info.copy(icon = Key.key(cauldron.blockType.key().value() + "_$level"))
            }
        }
        
        wailaInfoProvider<Cocoa>("cocoa") {
            blocks = registryEntrySetOf(BlockTypeKeys.COCOA)
            infoProvider(DEFAULT) { _, _, cocoa, info ->
                info.copy(icon = Key.key("cocoa_stage${cocoa.age}"))
            }
        }
        
        wailaInfoProvider<Comparator>("comparator") {
            blocks = registryEntrySetOf(BlockTypeKeys.COMPARATOR)
            infoProvider(DEFAULT) { _, _, comparator, info ->
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
            infoProvider(DEFAULT) { _, _, crop, info ->
                val maxTexStage = MAX_TEXTURE_STAGES[crop.blockType.typedKey]!!
                val stage = ((crop.age / crop.maximumAge.toDouble()) * maxTexStage).roundToInt()
                info.copy(icon = Key.key(crop.blockType.key().value() + "_stage$stage"))
            }
        }
        
        wailaInfoProvider<DaylightDetector>("daylight_detector") {
            blocks = registryEntrySetOf(BlockTypeKeys.DAYLIGHT_DETECTOR)
            infoProvider(DEFAULT) { _, _, detector, info ->
                if (detector.isInverted)
                    info.copy(icon = Key.key("daylight_detector_inverted"))
                else info
            }
        }
        
        wailaInfoProvider<DriedGhast>("dried_ghast") {
            blocks = registryEntrySetOf(BlockTypeKeys.DRIED_GHAST)
            infoProvider(DEFAULT) { _, _, driedGhast, info ->
                info.copy(icon = Key.key("dried_ghast_hydration_${driedGhast.hydration}"))
            }
        }
        
        wailaInfoProvider<Hatchable>("hatchable") {
            blocks = registryEntrySetOf(BlockTypeKeys.SNIFFER_EGG)
            infoProvider(DEFAULT) { _, _, hatchable, info ->
                info.copy(icon = Key.key("${hatchable.blockType.key().value()}_${hatchable.hatch}"))
            }
        }
        
        wailaInfoProvider<Lantern>("lantern") {
            blocks = registryEntrySetOf(BlockTypeTagKeys.LANTERNS)
            infoProvider(DEFAULT) { _, _, lantern, info ->
                if (lantern.isHanging)
                    info.copy(icon = Key.key(lantern.blockType.key().value() + "_hanging"))
                else info
            }
        }
        
        wailaInfoProvider<Lightable>("redstone_lamp") {
            blocks = registryEntrySetOf(BlockTypeKeys.REDSTONE_LAMP)
            infoProvider(DEFAULT) { _, _, lightable, info ->
                if (lightable.isLit)
                    info.copy(icon = Key.key("redstone_lamp_on"))
                else info
            }
        }
        
        wailaInfoProvider<RedstoneRail>("redstone_rail") {
            blocks = registryEntrySetOf(BlockTypeKeys.ACTIVATOR_RAIL, BlockTypeKeys.DETECTOR_RAIL, BlockTypeKeys.POWERED_RAIL)
            infoProvider(DEFAULT) { _, _, rail, info ->
                info.copy(icon = Key.key(rail.blockType.key().value() + if (rail.isPowered) "_on" else ""))
            }
        }
        
        wailaInfoProvider<Repeater>("repeater") {
            blocks = registryEntrySetOf(BlockTypeKeys.REPEATER)
            infoProvider(DEFAULT) { _, _, repeater, info ->
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
            infoProvider(DEFAULT) { _, _, anchor, info ->
                info.copy(icon = Key.key("respawn_anchor_${anchor.charges}"))
            }
        }
        
        wailaInfoProvider<SeaPickle>("sea_pickle") {
            blocks = registryEntrySetOf(BlockTypeKeys.SEA_PICKLE)
            infoProvider(DEFAULT) { _, _, pickle, info ->
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
            infoProvider(DEFAULT) { _, _, testBlock, info ->
                info.copy(icon = Key.key("test_block_${testBlock.mode.name.lowercase()}"))
            }
        }
    }
    
}

//<editor-fold desc="tool icons">
private val CHECK_MARK = Component.text("✔", NamedTextColor.GREEN)
private val CROSS = Component.text("❌", NamedTextColor.RED)

private val TOOL_ICONS: Map<BlockType, List<Component>>
    by combinedProvider(
        NovaRegistries.WAILA_TOOL_ICON_PROVIDER.entrySet, BlockTypeTags.TAGS_BY_ELEMENT
    ) { iconProviders, tagsByBlockType ->
        combinedProvider(
            tagsByBlockType.map { [blockType, tags] ->
                combinedProvider(
                    iconProviders
                        .flatMapTo(LinkedHashSet()) { it.getIcon(tags) }
                        .map(TextureIconContent::getIcon)
                ) { icons ->
                    val components = icons.mapNotNull { icon ->
                        icon
                            ?.component
                            ?.shadowColor(ShadowColor.none())
                            ?.let { component -> MovedFonts.moveVertically(component, 1) }
                    }
                    blockType to components
                }
            }
        ) { it.toMap() }
    }.flatten()

private fun getToolText(player: Player, block: Block): Component {
    val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
    return getToolText(
        player,
        TOOL_ICONS[block.blockType] ?: emptyList(),
        block.blockType.hardness.toDouble(),
        block.isPreferredTool(tool ?: ItemStack.empty())
    )
}

internal fun getCustomItemServiceToolText(player: Player, block: Block): Component {
    val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
    return getToolText(
        player,
        emptyList(),
        1.0,
        CustomItemServiceManager.canBreakBlock(block, tool)
    )
}

private fun getToolText(
    player: Player,
    toolIcons: List<Component>,
    hardness: Double,
    correctToolForDrops: Boolean?
): Component {
    val builder = Component.text()
    
    if (hardness < 0)
        return builder.append(CROSS).build()
    
    if (player.gameMode == GameMode.CREATIVE || correctToolForDrops == true) {
        builder.append(CHECK_MARK)
    } else if (correctToolForDrops != null) {
        builder.append(CROSS)
    }
    
    toolIcons.forEach(builder::append)
    
    return builder.build()
}
//</editor-fold>

//<editor-fold desc="energy bar">
private val ENERGY_BAR_BACKGROUND_COLOR = TextColor.color(0x595959)
private val ENERGY_BAR_FILL_COLOR = TextColor.color(0xEB0000)
private const val ENERGY_BAR_BORDER_WIDTH = 1

private val ENERGY_BALANCED_INDICATOR = Component
    .text("↑↓", NamedTextColor.DARK_GRAY)
    .shadowColor(ShadowColor.none())

private val ENERGY_LARGE_INCREASE_SMALL_DECREASE_INDICATOR = Component.text()
    .color(NamedTextColor.DARK_GRAY)
    .shadowColor(ShadowColor.none())
    .append(Component.text("↑", null, TextDecoration.BOLD))
    .append(Component.text("↓"))
    .build()

private val ENERGY_SMALL_INCREASE_LARGE_DECREASE_INDICATOR = Component
    .text()
    .color(NamedTextColor.DARK_GRAY)
    .shadowColor(ShadowColor.none())
    .append(Component.text("↑"))
    .append(Component.text("↓", null, TextDecoration.BOLD))
    .build()

private val ENERGY_ONLY_INCREASE_INDICATOR = Component
    .text("↑", NamedTextColor.DARK_GRAY)
    .shadowColor(ShadowColor.none())

private val ENERGY_ONLY_DECREASE_INDICATOR = Component
    .text("↓", NamedTextColor.DARK_GRAY)
    .shadowColor(ShadowColor.none())

private fun createEnergyBar(holder: DefaultEnergyHolder, width: Float): Component {
    val indicator = when {
        holder.energyPlus == holder.energyMinus && holder.energyPlus == 0L -> null
        holder.energyPlus == holder.energyMinus -> ENERGY_BALANCED_INDICATOR
        holder.energyPlus == 0L -> ENERGY_ONLY_DECREASE_INDICATOR
        holder.energyMinus == 0L -> ENERGY_ONLY_INCREASE_INDICATOR
        holder.energyPlus > holder.energyMinus -> ENERGY_LARGE_INCREASE_SMALL_DECREASE_INDICATOR
        holder.energyPlus < holder.energyMinus -> ENERGY_SMALL_INCREASE_LARGE_DECREASE_INDICATOR
        else -> null
    }
    val middlePieceCount = ((width - WailaEnergyBarTextures.END_PIECE_WIDTH * 2) / WailaEnergyBarTextures.MIDDLE_PIECE_WIDTH).roundToInt().coerceAtLeast(1)
    val barWidth = WailaEnergyBarTextures.END_PIECE_WIDTH * 2 + middlePieceCount * WailaEnergyBarTextures.MIDDLE_PIECE_WIDTH
    val percentage = (holder.energy.toDouble() / holder.maxEnergy.toDouble()).coerceIn(0.0, 1.0)
    val fillWidth = ((barWidth - ENERGY_BAR_BORDER_WIDTH * 2) * percentage).roundToInt()
    
    val builder = Component.text()
        .move(-1)
        .append(createSolidEnergyBar(barWidth, ENERGY_BAR_BACKGROUND_COLOR))
        .move(-(barWidth - ENERGY_BAR_BORDER_WIDTH))
        .append(createSolidEnergyBar(fillWidth, ENERGY_BAR_FILL_COLOR))
        .move(-(fillWidth + ENERGY_BAR_BORDER_WIDTH))
        .append(createEnergyBarForeground(middlePieceCount))
        .append(Component.text(" "))
    
    if (indicator != null)
        builder.append(indicator)
    
    return builder.build()
}

private fun createSolidEnergyBar(width: Int, color: TextColor): Component {
    val builder = Component.text()
    repeat(width) {
        builder.appendEnergyBarChar(WailaEnergyBarTextures.barPart, 1, color)
    }
    return builder.build()
}

private fun createEnergyBarForeground(middlePieceCount: Int): Component {
    val builder = Component.text()
    builder.appendEnergyBarChar(
        WailaEnergyBarTextures.start,
        WailaEnergyBarTextures.END_PIECE_WIDTH
    )
    
    repeat(middlePieceCount) { index ->
        builder.appendEnergyBarChar(
            WailaEnergyBarTextures.middle(index),
            WailaEnergyBarTextures.MIDDLE_PIECE_WIDTH
        )
    }
    
    builder.appendEnergyBarChar(
        WailaEnergyBarTextures.end,
        WailaEnergyBarTextures.END_PIECE_WIDTH
    )
    return builder.build()
}

private fun TextComponent.Builder.appendEnergyBarChar(char: FontChar, width: Int, color: TextColor? = null) {
    var component = char.component.shadowColor(ShadowColor.none())
    if (color != null)
        component = component.color(color)
    
    append(component)
    move(width - char.width)
}
//</editor-fold>

//<editor-fold desc="waila icons">
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
//</editor-fold>