package xyz.xenondevs.nova.ui.waila.info

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.tags
import xyz.xenondevs.nova.resources.builder.task.TextureIconContent
import xyz.xenondevs.nova.ui.overlay.MovedFonts
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.blockType

private val CHECK_MARK = Component.text("✔", NamedTextColor.GREEN)
private val CROSS = Component.text("❌", NamedTextColor.RED)

internal object ToolText {
    
    fun getToolText(player: Player, block: Block): Component {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolText(
            player,
            block.blockType.tags.get(),
            block.blockType.hardness.toDouble(),
            block.isPreferredTool(tool ?: ItemStack.empty())
        )
    }
    
    fun getCustomItemServiceToolText(player: Player, block: Block): Component {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolText(
            player,
            emptySet(),
            1.0,
            CustomItemServiceManager.canBreakBlock(block, tool)
        )
    }
    
    fun getToolText(
        player: Player,
        tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>,
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
        
        NovaRegistries.WAILA_TOOL_ICON_PROVIDER.entrySet.get()
            .flatMapTo(LinkedHashSet()) { it.getIcon(tags) }
            .mapNotNull { 
                TextureIconContent.getIcon(it)
                    ?.component
                    ?.shadowColor(ShadowColor.none())
                    ?.let { c -> MovedFonts.moveVertically(c, 1) }
            }
            .forEach(builder::append)
        
        return builder.build()
    }
    
}
