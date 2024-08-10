package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos
import kotlin.math.roundToInt

private val MAX_TEXTURE_STAGE = mapOf(
    Material.BEETROOTS to 3,
    Material.CARROTS to 3,
    Material.NETHER_WART to 2,
    Material.POTATOES to 3,
    Material.WHEAT to 7,
    Material.SWEET_BERRY_BUSH to 3,
    Material.TORCHFLOWER_CROP to 2
)

internal object CropWailaInfoProvider : VanillaWailaInfoProvider<Ageable>(MAX_TEXTURE_STAGE.keys) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Ageable): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        val stage = ((blockState.age / blockState.maximumAge.toDouble()) * MAX_TEXTURE_STAGE[blockState.material]!!).roundToInt()
        info.icon = ResourceLocation.withDefaultNamespace(blockState.material.name.lowercase() + "_stage$stage")
        return info
    }
    
}