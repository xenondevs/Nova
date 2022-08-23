package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Cake
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object CakeWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.CAKE)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val bites = (block.blockData as Cake).bites
        info.icon = NamespacedId("minecraft", if (bites == 0) "cake" else "cake_slice$bites")
        return info
    }
    
}