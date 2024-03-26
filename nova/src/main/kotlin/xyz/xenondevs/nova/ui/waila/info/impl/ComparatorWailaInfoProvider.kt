package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Comparator
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object ComparatorWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.COMPARATOR)) {
    
    override fun getInfo(player: Player, pos: BlockPos, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, block)
        val comparator = block.blockData as Comparator
        info.icon = getComparatorIcon(comparator)
        return info
    }
    
    private fun getComparatorIcon(comparator: Comparator): ResourceLocation {
        return ResourceLocation(
            "minecraft",
            "comparator"
                + (if (comparator.isPowered) "_on" else "")
                + (if (comparator.mode == Comparator.Mode.SUBTRACT) "_subtract" else "")
        )
        
    }
    
}