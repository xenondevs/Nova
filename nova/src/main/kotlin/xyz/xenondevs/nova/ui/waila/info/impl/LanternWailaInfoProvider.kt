package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.type.Lantern
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object LanternWailaInfoProvider : VanillaWailaInfoProvider<Lantern>(
    setOf(
        Material.LANTERN,
        Material.SOUL_LANTERN,
        Material.COPPER_LANTERN, Material.EXPOSED_COPPER_LANTERN, Material.WEATHERED_COPPER_LANTERN, Material.OXIDIZED_COPPER_LANTERN,
        Material.WAXED_COPPER_LANTERN, Material.WAXED_EXPOSED_COPPER_LANTERN, Material.WAXED_WEATHERED_COPPER_LANTERN, Material.WAXED_OXIDIZED_COPPER_LANTERN
    )
) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Lantern): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        if (blockState.isHanging) {
            info.icon = Key.key(blockState.material.name.lowercase() + "_hanging")
        }
        return info
    }
    
}