package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Comparator
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.WailaLine.Alignment
import xyz.xenondevs.nova.util.data.ComponentWidthBuilder

object ComparatorWailaInfoProvider : VanillaWailaInfoProvider(listOf(Material.COMPARATOR)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        
        val comparator = block.blockData as Comparator
        info.icon = getComparatorIcon(comparator)
        
        info.lines += WailaLine(
            ComponentWidthBuilder(player.locale)
                .append(TranslatableComponent("waila.nova.comparator.mode." + comparator.mode.name.lowercase()))
                .color(ChatColor.GRAY)
                .create(),
            Alignment.CENTERED
        )
        
        return info
    }
    
    private fun getComparatorIcon(comparator: Comparator): NamespacedId {
        return NamespacedId(
            "minecraft",
            "comparator"
                + (if (comparator.isPowered) "_on" else "")
                + (if (comparator.mode == Comparator.Mode.SUBTRACT) "_subtract" else "")
        )
        
    }
    
}