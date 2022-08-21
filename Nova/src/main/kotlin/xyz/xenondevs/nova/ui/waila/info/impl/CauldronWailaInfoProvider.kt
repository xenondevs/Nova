package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object CauldronWailaInfoProvider : VanillaWailaInfoProvider(
    listOf(
        Material.WATER_CAULDRON,
        Material.POWDER_SNOW_CAULDRON
    )
) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val levelled = block.blockData as Levelled
        val level = when(levelled.level) {
            1 -> "level1"
            2 -> "level2"
            3 -> "full"
            else -> throw IllegalStateException("Cauldron level is not 1, 2 or 3")
        }
        info.icon = NamespacedId("minecraft", block.type.name.lowercase() + "_$level")
        return info
    }
    
}