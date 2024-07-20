package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.type.SeaPickle
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object SeaPickleWailaInfoProvider : VanillaWailaInfoProvider<SeaPickle>(
    setOf(Material.SEA_PICKLE)
) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: SeaPickle): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = ResourceLocation.withDefaultNamespace(getSeaPickleName(blockState))
        return info
    }
    
    private fun getSeaPickleName(pickle: SeaPickle): String {
        val amount = pickle.pickles
        if (amount > 1) {
            val prefix = when (amount) {
                2 -> "two"
                3 -> "three"
                4 -> "four"
                else -> throw IllegalStateException("Invalid amount: $amount")
            }
            
            return prefix + (if (!pickle.isWaterlogged) "_dead_" else "_") + "sea_pickles"
        }
        
        return (if (!pickle.isWaterlogged) "dead_" else "") + "sea_pickle"
    }
    
}