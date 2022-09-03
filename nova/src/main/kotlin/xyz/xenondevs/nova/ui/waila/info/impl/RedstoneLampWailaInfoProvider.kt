package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Lightable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object RedstoneLampWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.REDSTONE_LAMP)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val lamp = block.blockData as Lightable
        if (lamp.isLit) {
            info.icon = NamespacedId("minecraft", "redstone_lamp_on")
        }
        
        return info
    }
    
}