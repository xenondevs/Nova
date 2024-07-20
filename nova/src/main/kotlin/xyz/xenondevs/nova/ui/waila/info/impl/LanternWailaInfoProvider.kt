package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.type.Lantern
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object LanternWailaInfoProvider : VanillaWailaInfoProvider<Lantern>(setOf(Material.LANTERN, Material.SOUL_LANTERN)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Lantern): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        if (blockState.isHanging) {
            info.icon = ResourceLocation.withDefaultNamespace(blockState.material.name.lowercase() + "_hanging")
        }
        return info
    }
    
}