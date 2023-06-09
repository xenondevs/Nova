package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Hatchable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object HatchableWailaInfoProvider : VanillaWailaInfoProvider(setOf(Material.SNIFFER_EGG)) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        val hatchable = block.blockData as Hatchable
        info.icon = ResourceLocation("minecraft", "${block.type.name.lowercase()}_${hatchable.hatch}")
        return info
    }
    
}