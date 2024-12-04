package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object RespawnAnchorWailaInfoProvider : VanillaWailaInfoProvider<RespawnAnchor>(setOf(Material.RESPAWN_ANCHOR)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: RespawnAnchor): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = Key.key("respawn_anchor_${blockState.charges}")
        return info
    }
    
}