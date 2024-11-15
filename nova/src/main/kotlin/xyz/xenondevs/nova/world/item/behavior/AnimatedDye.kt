package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.DyedItemColor
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.getMod
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.config.entryOrElse
import xyz.xenondevs.nova.util.data.ImageUtils
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty
import java.awt.Color

/**
 * Creates a new [AnimatedDye].
 *
 * @param defaultTicksPerColor The default value for the amount of ticks between each color,
 * to be used when `ticks_per_color` is not present in the config, or null to require config presence.
 * @param defaultColors The default value for the list of colors to cycle through,
 * to be used when `colors` is not present in the config, or null to require config presence.
 */
fun AnimatedDye(
    defaultTicksPerColor: Int? = null,
    defaultColors: List<Color>? = null
) = ItemBehaviorFactory<AnimatedDye> {
    val config = it.config
    AnimatedDye(
        config.entryOrElse(defaultTicksPerColor, "ticks_per_color"),
        config.entryOrElse(defaultColors, "colors")
    )
}

class AnimatedDye(
    ticksPerColor: Provider<Int>,
    colors: Provider<List<Color>>
) : ItemBehavior {
    
    private val componentFrames: Provider<List<DataComponentMap>> = combinedProvider(
        ticksPerColor, colors
    ) { ticksPerFrame, frames ->
        buildList {
            for (i in 0..<(frames.size * ticksPerFrame)) {
                val from = frames[i / ticksPerFrame]
                val to = frames[(i / ticksPerFrame + 1) % frames.size]
                val color = ImageUtils.lerp(from, to, i % ticksPerFrame / ticksPerFrame.toFloat())
                
                this += DataComponentMap.builder()
                    .set(DataComponents.DYED_COLOR, DyedItemColor(color.rgb, false))
                    .build()
            }
        }
    }
    
    override val baseDataComponents = componentFrames.getMod(EquipmentAnimator.tick)
    override val vanillaMaterialProperties = provider(listOf(VanillaMaterialProperty.DYEABLE))
    
    init {
        EquipmentAnimator.animatedBehaviors += this
    }
    
}