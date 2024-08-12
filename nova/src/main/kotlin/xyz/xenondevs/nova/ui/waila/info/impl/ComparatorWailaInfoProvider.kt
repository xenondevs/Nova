package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.type.Comparator
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object ComparatorWailaInfoProvider : VanillaWailaInfoProvider<Comparator>(setOf(Material.COMPARATOR)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Comparator): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = getComparatorIcon(blockState)
        return info
    }
    
    private fun getComparatorIcon(comparator: Comparator): ResourceLocation {
        return ResourceLocation.withDefaultNamespace(
            "comparator"
                + (if (comparator.isPowered) "_on" else "")
                + (if (comparator.mode == Comparator.Mode.SUBTRACT) "_subtract" else "")
        )
        
    }
    
}