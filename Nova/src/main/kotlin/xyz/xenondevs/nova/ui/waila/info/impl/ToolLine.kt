package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.util.data.ComponentWidthBuilder
import xyz.xenondevs.nova.util.hardness
import xyz.xenondevs.nova.util.item.ToolCategory
import xyz.xenondevs.nova.util.item.ToolLevel
import xyz.xenondevs.nova.util.item.ToolUtils

private const val CHECK_MARK = "✔"
private const val CROSS = "❌"

typealias WidthLine = Pair<Array<BaseComponent>, Int>

internal object ToolLine {
    
    fun getToolLine(player: Player, block: Block): WidthLine {
        val tool = player.inventory.itemInMainHand
        val blockType = block.type
        val blockCategories = ToolCategory.ofBlock(blockType)
        val blockLevel = ToolLevel.ofBlock(blockType)
        val toolType = tool.type
        
        val builder = ComponentWidthBuilder(player.locale)
        if (block.hardness < 0) {
            return builder
                .append(TranslatableComponent("waila.nova.required_tool.unbreakable"))
                .color(ChatColor.RED)
                .create()
        }
        
        val canBreak = player.gameMode == GameMode.CREATIVE || ToolUtils.isCorrectToolForDrops(toolType, block)
        
        if (blockCategories.isNotEmpty()) {
            builder.append(TranslatableComponent("waila.nova.required_tool")).color(ChatColor.GRAY)
            
            blockCategories.forEach {
                builder.append(getToolIcon(blockLevel ?: ToolLevel.WOODEN, it)).color(ChatColor.WHITE)
            }
            
            builder.append(" ").font("default")
            
            if (canBreak) builder.append(CHECK_MARK).color(ChatColor.GREEN)
            else builder.append(CROSS).color(ChatColor.RED)
        } else {
            builder.append(TranslatableComponent("waila.nova.required_tool.none")).color(ChatColor.GRAY)
        }
        
        return builder.create()
    }
    
    private fun getToolIcon(level: ToolLevel, category: ToolCategory): TextComponent {
        val itemName = if (category == ToolCategory.SHEARS)
            "minecraft:item/shears"
        else "minecraft:item/${level.name.lowercase()}_${category.name.lowercase()}"
        
        val fontChar = Resources.getTextureIconChar(itemName)
        return TextComponent(fontChar.char.toString()).apply { color = ChatColor.WHITE; font = fontChar.font }
    }
    
}