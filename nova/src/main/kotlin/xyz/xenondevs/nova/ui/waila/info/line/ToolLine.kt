package xyz.xenondevs.nova.ui.waila.info.line

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolTier
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolUtils
import xyz.xenondevs.nova.util.item.takeUnlessEmpty

private val CHECK_MARK = Component.text("✔", NamedTextColor.GREEN)
private val CROSS = Component.text("❌", NamedTextColor.RED)

object ToolLine {
    
    fun getToolLine(player: Player, block: Block): WailaLine {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolLine(
            player,
            ToolCategory.ofBlock(block),
            ToolTier.ofBlock(block),
            block.hardness,
            ToolUtils.isCorrectToolForDrops(block, tool)
        )
    }
    
    fun getCustomItemServiceToolLine(player: Player, block: Block): WailaLine {
        val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
        return getToolLine(
            player,
            null,
            null,
            1.0,
            CustomItemServiceManager.canBreakBlock(block, tool)
        )
    }
    
    fun getToolLine(
        player: Player,
        blockToolCategories: List<ToolCategory>?,
        blockToolLevel: ToolTier?,
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
        
        if (blockToolCategories == null) {
            appendCanBreak()
        } else if (blockToolCategories.isNotEmpty()) {
            builder.append(Component.translatable("waila.nova.required_tool", NamedTextColor.GRAY))
            blockToolCategories.forEach { builder.append(getToolIcon(blockToolLevel, it)) }
            appendCanBreak()
        } else {
            builder.append(Component.translatable("waila.nova.required_tool.none", NamedTextColor.GRAY))
        }
        
        return WailaLine(builder.build(), Alignment.CENTERED)
    }
    
    private fun getToolIcon(tier: ToolTier?, category: ToolCategory): Component =
        NovaRegistries.WAILA_TOOL_ICON_PROVIDER
            .firstNotNullOfOrNull { it.getIcon(category, tier) }
            ?.let { ResourceLookups.TEXTURE_ICON_LOOKUP.getOrThrow(it) }
            ?.component
            ?: Component.empty()
    
}