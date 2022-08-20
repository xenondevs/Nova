package xyz.xenondevs.nova.ui.waila.info.impl

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.type.Candle
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo

internal object CandleWailaInfoProvider : VanillaWailaInfoProvider(
    listOf(
        Material.CANDLE, Material.WHITE_CANDLE, Material.ORANGE_CANDLE, Material.MAGENTA_CANDLE, Material.LIGHT_BLUE_CANDLE,
        Material.YELLOW_CANDLE, Material.LIME_CANDLE, Material.PINK_CANDLE, Material.GRAY_CANDLE, Material.LIGHT_GRAY_CANDLE,
        Material.CYAN_CANDLE, Material.PURPLE_CANDLE, Material.BLUE_CANDLE, Material.BROWN_CANDLE, Material.GREEN_CANDLE,
        Material.RED_CANDLE, Material.BLACK_CANDLE
    )
) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val defaultInfo = DefaultVanillaWailaInfoProvider.getInfo(player, block)
        return WailaInfo(getCandleId(block), defaultInfo.text, defaultInfo.widths)
    }
    
    private fun getCandleId(block: Block): NamespacedId {
        val name = block.type.name.lowercase()
        val candle = block.blockData as Candle
        val amount = when (candle.candles) {
            1 -> "one_candle"
            2 -> "two_candles"
            3 -> "three_candles"
            4 -> "four_candles"
            else -> IllegalStateException("Invalid amount of candles")
        }
        
        return NamespacedId("minecraft", "${name}_${amount}")
    }
    
}