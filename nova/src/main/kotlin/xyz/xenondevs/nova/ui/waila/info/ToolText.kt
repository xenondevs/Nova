package xyz.xenondevs.nova.ui.waila.info

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.blockType

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

internal object ToolText {
    
    fun getToolText(player: Player, block: Block): Component {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolText(
            player,
            TOOL_ICONS[block.blockType] ?: emptyList(),
            block.blockType.hardness.toDouble(),
            block.isPreferredTool(tool ?: ItemStack.empty())
        )
    }
    
    fun getCustomItemServiceToolText(player: Player, block: Block): Component {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolText(
            player,
            emptyList(),
            1.0,
            CustomItemServiceManager.canBreakBlock(block, tool)
        )
    }
    
    fun getToolText(
        player: Player,
        toolIcons: List<Component>,
        hardness: Double,
        correctToolForDrops: Boolean?
    ): Component {
        val builder = Component.text()
        
        // unbreakable
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
    
}
