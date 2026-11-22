package xyz.xenondevs.nova.ui.waila

import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.PistonHead
import org.bukkit.block.data.type.TechnicalPiston
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.registry.NovaRegistrar.wailaInfoProvider
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.builder.task.WailaEnergyBarTextures
import xyz.xenondevs.nova.util.capitalizeAll
import xyz.xenondevs.nova.util.component.adventure.move
import xyz.xenondevs.nova.util.component.adventure.toMinecraftLocaleCode
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.isNovaTileEntity
import xyz.xenondevs.nova.world.block.name
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import kotlin.math.roundToInt

@InternalInit(
    stage = BOOTSTRAP,
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
            
            return@infoProvider WailaInfo(WailaIconProvider.getRenderedIcon(blockState), lines)
        }
    }
    
}

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

//<editor-fold desc="block names">
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