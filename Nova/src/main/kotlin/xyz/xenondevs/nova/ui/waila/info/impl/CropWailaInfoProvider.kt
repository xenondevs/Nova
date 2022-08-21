package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import kotlin.math.roundToInt

private val MAX_TEXTURE_STAGE = mapOf(
    Material.BEETROOTS to 3,
    Material.CARROTS to 3,
    Material.NETHER_WART to 2,
    Material.POTATOES to 3,
    Material.WHEAT to 7,
    Material.SWEET_BERRY_BUSH to 3
)

object CropWailaInfoProvider : VanillaWailaInfoProvider(
    listOf(
        Material.BEETROOTS, Material.CARROTS, Material.NETHER_WART, Material.POTATOES,
        Material.SWEET_BERRY_BUSH, Material.WHEAT
    )
) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        
        val ageable = block.blockData as Ageable
        val stage = ((ageable.age / ageable.maximumAge.toDouble()) * MAX_TEXTURE_STAGE[block.type]!!).roundToInt()
        
        info.icon = NamespacedId("minecraft", block.type.name.lowercase() + "_stage$stage")
        return info
    }

}