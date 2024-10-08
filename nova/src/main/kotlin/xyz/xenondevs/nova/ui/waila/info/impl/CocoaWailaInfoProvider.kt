package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.type.Cocoa
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object CocoaWailaInfoProvider : VanillaWailaInfoProvider<Cocoa>(setOf(Material.COCOA)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Cocoa): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = ResourceLocation.withDefaultNamespace("cocoa_stage$${blockState.age}")
        return info
    }
    
}