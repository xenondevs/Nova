package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.type.Cake
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object CakeWailaInfoProvider : VanillaWailaInfoProvider<Cake>(setOf(Material.CAKE)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Cake): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        val bites = blockState.bites
        info.icon = Key.key(if (bites == 0) "cake" else "cake_slice$bites")
        return info
    }
    
}