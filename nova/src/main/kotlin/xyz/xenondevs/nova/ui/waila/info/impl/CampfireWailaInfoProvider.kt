package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Campfire
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object CampfireWailaInfoProvider : VanillaWailaInfoProvider(
    setOf(Material.CAMPFIRE, Material.SOUL_CAMPFIRE)
) {
    
    override fun getInfo(player: Player, pos: BlockPos, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, block)
        val lit = (block.blockData as Campfire).isLit
        info.icon = ResourceLocation("minecraft", if (lit) block.type.name.lowercase() else "campfire_off")
        return info
    }
    
}