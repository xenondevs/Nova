package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object CampfireWailaInfoProvider : VanillaWailaInfoProvider(
    setOf(Material.CAMPFIRE, Material.SOUL_CAMPFIRE)
) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val lit = (block.blockData as Campfire).isLit
        info.icon = NamespacedId("minecraft", if (lit) block.type.name.lowercase() else "campfire_off")
        return info
    }
    
}