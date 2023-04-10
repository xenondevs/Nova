package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object RespawnAnchorWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.RESPAWN_ANCHOR)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val anchor = block.blockData as RespawnAnchor
        info.icon = ResourceLocation("minecraft", "respawn_anchor_${anchor.charges}")
        return info
    }
    
}