package xyz.xenondevs.nova.data.resources.model.blockstate

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DaylightDetectorBlock
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Material
import xyz.xenondevs.bytebase.util.hasMask
import xyz.xenondevs.nova.util.intValue

internal class DaylightDetectorBlockStateConfig(
    val power: Int,
    val inverted: Boolean
) : BlockStateConfig {
    
    override val type = DaylightDetectorBlockStateConfig
    override val id = getIdOf(power, inverted)
    override val variantString = "inverted=$inverted,power=$power"
    override val blockState: BlockState = Blocks.DAYLIGHT_DETECTOR.defaultBlockState()
        .setValue(DaylightDetectorBlock.POWER, power)
        .setValue(DaylightDetectorBlock.INVERTED, inverted)
    
    init {
        require(power in 0..15)
    }
    
    companion object : DynamicDefaultingBlockStateConfigType<DaylightDetectorBlockStateConfig>() {
        
        override val maxId = 30
        override val fileName = "daylight_detector"
        override val material = Material.DAYLIGHT_DETECTOR
        override val blockedIds = setOf(0x00, 0x10) // states with power=0
        
        fun getIdOf(power: Int, inverted: Boolean): Int {
            return power and (inverted.intValue shl 4)
        }
        
        override fun of(id: Int): DaylightDetectorBlockStateConfig {
            return DaylightDetectorBlockStateConfig(
                power = id and 0xF,
                inverted = id.hasMask(0x10)
            )
        }
        
        override fun of(variantString: String): DaylightDetectorBlockStateConfig {
            val properties = variantString.split(',')
                .associate { val s = it.split('='); s[0] to s[1] }
            
            return DaylightDetectorBlockStateConfig(
                properties["power"]?.toInt() ?: 0,
                properties["inverted"]?.toBoolean() ?: false
            )
        }
        
    }
    
}