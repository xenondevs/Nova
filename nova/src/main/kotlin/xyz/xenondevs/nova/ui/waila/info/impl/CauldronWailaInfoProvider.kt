package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object CauldronWailaInfoProvider : VanillaWailaInfoProvider<Levelled>(
    setOf(
        Material.WATER_CAULDRON,
        Material.POWDER_SNOW_CAULDRON
    )
) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Levelled): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        val level = when (blockState.level) {
            1 -> "level1"
            2 -> "level2"
            3 -> "full"
            else -> throw IllegalStateException("Cauldron level is not 1, 2 or 3")
        }
        info.icon = ResourceLocation.withDefaultNamespace(blockState.material.name.lowercase() + "_$level")
        return info
    }
    
}