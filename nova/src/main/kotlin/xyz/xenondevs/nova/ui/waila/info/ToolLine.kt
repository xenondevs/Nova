package xyz.xenondevs.nova.ui.waila.info

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.tags
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.blockType

private val CHECK_MARK = Component.text("✔", NamedTextColor.GREEN)
private val CROSS = Component.text("❌", NamedTextColor.RED)

object ToolLine {
    
    fun getToolLine(player: Player, block: Block): WailaLine {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolLine(
            player,
            block.blockType.tags.get(),
            block.blockType.hardness.toDouble(),
            block.isPreferredTool(tool ?: ItemStack.empty())
        )
    }
    
    fun getCustomItemServiceToolLine(player: Player, block: Block): WailaLine {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolLine(
            player,
            emptySet(),
            1.0,
            CustomItemServiceManager.canBreakBlock(block, tool)
        )
    }
    
    fun getToolLine(
        player: Player,
        tags: Set<RegistryEntrySet.Paper.Tag<BlockType>>,
        hardness: Double,
        correctToolForDrops: Boolean?
    ): WailaLine {
        val builder = Component.text()
        if (hardness < 0) {
            return WailaLine(
                builder
                    .append(Component.translatable("waila.nova.required_tool.unbreakable", NamedTextColor.RED))
                    .build(),
                Alignment.CENTERED
            )
        }
        
        fun appendCanBreak() {
            builder.append(Component.space())
            if (player.gameMode == GameMode.CREATIVE || correctToolForDrops == true) {
                builder.append(CHECK_MARK)
            } else if (correctToolForDrops != null) {
                builder.append(CROSS)
            }
        }
        
        val toolIcons = NovaRegistries.WAILA_TOOL_ICON_PROVIDER.entrySet.get()
            .flatMapTo(LinkedHashSet()) { it.getIcon(tags) }
            .map { ResourceLookups.textureIcon.getValue(it).component }
        
        if (toolIcons.isEmpty()) {
            appendCanBreak()
        } else {
            builder.append(Component.translatable("waila.nova.required_tool", NamedTextColor.GRAY))
            toolIcons.forEach(builder::append)
            appendCanBreak()
        }
        
        return WailaLine(builder.build(), Alignment.CENTERED)
    }
    
}
