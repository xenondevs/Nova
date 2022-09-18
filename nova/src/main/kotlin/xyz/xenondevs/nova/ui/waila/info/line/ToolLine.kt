package xyz.xenondevs.nova.ui.waila.info.line

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.item.tool.ToolCategory
import xyz.xenondevs.nova.item.tool.ToolLevel
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.util.data.ComponentWidthBuilder
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolUtils

private const val CHECK_MARK = "✔"
private const val CROSS = "❌"

object ToolLine {
    
    fun getToolLine(player: Player, block: Block): WailaLine {
        val tool = player.inventory.itemInMainHand
        return getToolLine(
            player,
            ToolCategory.ofBlock(block),
            ToolLevel.ofBlock(block),
            block.hardness,
            ToolUtils.isCorrectToolForDrops(tool, block)
        )
    }
    
    fun getToolLine(
        player: Player,
        blockToolCategories: List<ToolCategory>,
        blockToolLevel: ToolLevel?,
        hardness: Double,
        correctToolForDrops: Boolean
    ): WailaLine {
        val builder = ComponentWidthBuilder(player.locale)
        if (hardness < 0) {
            return WailaLine(
                builder
                    .append(TranslatableComponent("waila.nova.required_tool.unbreakable"))
                    .color(ChatColor.RED)
                    .create(),
                Alignment.CENTERED
            )
        }
        
        val canBreak = player.gameMode == GameMode.CREATIVE || correctToolForDrops
        
        if (blockToolCategories.isNotEmpty()) {
            builder.append(TranslatableComponent("waila.nova.required_tool")).color(ChatColor.GRAY)
            
            blockToolCategories.forEach {
                builder.append(getToolIcon(blockToolLevel, it), 16).color(ChatColor.WHITE)
            }
            
            builder.append(" ").font("default")
            
            if (canBreak) builder.append(CHECK_MARK).color(ChatColor.GREEN)
            else builder.append(CROSS).color(ChatColor.RED)
        } else {
            builder.append(TranslatableComponent("waila.nova.required_tool.none")).color(ChatColor.GRAY)
        }
        
        return WailaLine(builder.create(), Alignment.CENTERED)
    }
    
    private fun getToolIcon(level: ToolLevel?, category: ToolCategory): TextComponent {
        val fontChar = Resources.getTextureIconChar(category.getIcon(level))
        return TextComponent(fontChar.char.toString()).apply { color = ChatColor.WHITE; font = fontChar.font }
    }
    
}