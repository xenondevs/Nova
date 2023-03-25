package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.SeaPickle
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object SeaPickleWailaInfoProvider : VanillaWailaInfoProvider(
    setOf(Material.SEA_PICKLE)
) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        info.icon = ResourceLocation("minecraft", getSeaPickleName(block))
        return info
    }
    
    private fun getSeaPickleName(block: Block): String {
        val pickle = block.blockData as SeaPickle
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