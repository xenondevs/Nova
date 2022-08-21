package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, block: NovaBlockState): WailaInfo {
        val material = block.material
        return WailaInfo(material.id, ArrayList())
    }
    
}