package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.type.DriedGhast
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object DriedGhastWailaInfoProvider : VanillaWailaInfoProvider<DriedGhast>(setOf(Material.DRIED_GHAST)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: DriedGhast): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = Key.key("dried_ghast_hydration_${blockState.hydration}")
        return info
    }
    
}