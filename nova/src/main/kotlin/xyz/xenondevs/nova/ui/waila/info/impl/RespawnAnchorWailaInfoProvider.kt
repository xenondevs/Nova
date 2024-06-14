package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.type.RespawnAnchor
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object RespawnAnchorWailaInfoProvider : VanillaWailaInfoProvider<RespawnAnchor>(setOf(Material.RESPAWN_ANCHOR)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: RespawnAnchor): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = ResourceLocation("minecraft", "respawn_anchor_${blockState.charges}")
        return info
    }
    
}