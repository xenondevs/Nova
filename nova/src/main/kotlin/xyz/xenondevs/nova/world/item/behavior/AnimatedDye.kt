package xyz.xenondevs.nova.world.item.behavior

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.DyedItemColor.dyedItemColor
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.getMod
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.data.ImageUtils
import xyz.xenondevs.nova.world.item.DataComponentMap
import xyz.xenondevs.nova.world.item.buildDataComponentMapProvider
import java.awt.Color

/**
 * Creates a factory for [AnimatedDye] behaviors using the given values, if not specified otherwise in the config.
 *
 * @param defaultTicksPerColor The default value for the number of ticks between each color.
 * Defaults to `1`.
 * Used when `ticks_per_color` is not specified in the config.
 *
 * @param defaultColors The default value for the list of colors to cycle through.
 * Defaults to an empty list.
 * Used when `colors` is not specified in the config.
 */
@Suppress("FunctionName")
fun AnimatedDye(
    defaultTicksPerColor: Int = 1,
    defaultColors: List<Color> = emptyList()
) = ItemBehaviorFactory { _, cfg ->
    AnimatedDye(
        cfg.entry(defaultTicksPerColor, "ticks_per_color"),
        cfg.entry(defaultColors, "colors")
    )
}

/**
 * Animates the `minecraft:dyed_color` component by interpolating between a given set of colors.
 *
 * @param ticksPerColor The amount of ticks between each color.
 * @param colors The list of colors to cycle through.
 */
class AnimatedDye(
    ticksPerColor: Provider<Int>,
    colors: Provider<List<Color>>
) : ItemBehavior {
    
    /**
     * The amount of ticks between each color.
     */
    val ticksPerColor: Int by ticksPerColor
    
    /**
     * The list of colors to cycle through.
     */
    val colors: List<Color> by colors
    
    private val componentFrames: Provider<List<DataComponentMap>> = combinedProvider(
        ticksPerColor, colors
    ) { ticksPerFrame, frames ->
        buildList {
            for (i in 0..<(frames.size * ticksPerFrame)) {
                val from = frames[i / ticksPerFrame]
                val to = frames[(i / ticksPerFrame + 1) % frames.size]
                val color = ImageUtils.lerp(from, to, i % ticksPerFrame / ticksPerFrame.toFloat())
                
                this += buildDataComponentMapProvider {
                    this[DataComponentTypes.DYED_COLOR] = dyedItemColor(org.bukkit.Color.fromARGB(color.rgb))
                }.get()
            }
        }
    }
    
    override val baseDataComponents = componentFrames.getMod(EquipmentAnimator.tick)
    
    init {
        EquipmentAnimator.animatedBehaviors += this
    }
    
    override fun toString(itemStack: ItemStack): String {
        return "AnimatedDye(ticksPerColor=$ticksPerColor, colors=$colors)"
    }
    
}