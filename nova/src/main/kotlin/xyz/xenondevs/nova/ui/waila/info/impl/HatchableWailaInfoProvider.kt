package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.Hatchable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object HatchableWailaInfoProvider : VanillaWailaInfoProvider<Hatchable>(setOf(Material.SNIFFER_EGG)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Hatchable): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = Key.key("${blockState.material.name.lowercase()}_${blockState.hatch}")
        return info
    }
    
}