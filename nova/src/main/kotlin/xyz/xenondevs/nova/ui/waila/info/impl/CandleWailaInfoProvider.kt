package xyz.xenondevs.nova.ui.waila.info.impl

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Candle
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

internal object CandleWailaInfoProvider : VanillaWailaInfoProvider<Candle>(
    setOf(
        Material.CANDLE, Material.WHITE_CANDLE, Material.ORANGE_CANDLE, Material.MAGENTA_CANDLE, Material.LIGHT_BLUE_CANDLE,
        Material.YELLOW_CANDLE, Material.LIME_CANDLE, Material.PINK_CANDLE, Material.GRAY_CANDLE, Material.LIGHT_GRAY_CANDLE,
        Material.CYAN_CANDLE, Material.PURPLE_CANDLE, Material.BLUE_CANDLE, Material.BROWN_CANDLE, Material.GREEN_CANDLE,
        Material.RED_CANDLE, Material.BLACK_CANDLE
    )
) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: Candle): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = getCandleId(blockState)
        return info
    }
    
    private fun getCandleId(candle: Candle): ResourceLocation {
        val name = candle.material.name.lowercase()
        val amount = when (candle.candles) {
            1 -> "one_candle"
            2 -> "two_candles"
            3 -> "three_candles"
            4 -> "four_candles"
            else -> IllegalStateException("Invalid amount of candles")
        }
        
        return ResourceLocation("minecraft", "${name}_${amount}")
    }
    
}